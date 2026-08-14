package dev.zorionten.portfolio.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
class GithubRepositoryService {
	private static final int MAX_README_CHARACTERS = 20_000;
	private static final Map<String, String> TECHNOLOGIES = Map.ofEntries(
			Map.entry("api gateway", "API Gateway"),
			Map.entry("aws lambda", "AWS Lambda"),
			Map.entry("docker compose", "Docker Compose"),
			Map.entry("electron", "Electron"),
			Map.entry("expo", "Expo"),
			Map.entry("express", "Express"),
			Map.entry("fastapi", "FastAPI"),
			Map.entry("fastify", "Fastify"),
			Map.entry("firebase", "Firebase"),
			Map.entry("flask", "Flask"),
			Map.entry("github actions", "GitHub Actions"),
			Map.entry("java", "Java"),
			Map.entry("javascript", "JavaScript"),
			Map.entry("jest", "Jest"),
			Map.entry("material ui", "Material UI"),
			Map.entry("mermaid", "Mermaid"),
			Map.entry("mongodb", "MongoDB"),
			Map.entry("nestjs", "NestJS"),
			Map.entry("node.js", "Node.js"),
			Map.entry("oauth", "OAuth"),
			Map.entry("opentelemetry", "OpenTelemetry"),
			Map.entry("phalcon", "Phalcon"),
			Map.entry("php", "PHP"),
			Map.entry("postgresql", "PostgreSQL"),
			Map.entry("python", "Python"),
			Map.entry("react native", "React Native"),
			Map.entry("react", "React"),
			Map.entry("redis", "Redis"),
			Map.entry("rust", "Rust"),
			Map.entry("socket.io", "Socket.IO"),
			Map.entry("spring boot", "Spring Boot"),
			Map.entry("supabase", "Supabase"),
			Map.entry("tailwind", "Tailwind CSS"),
			Map.entry("tauri", "Tauri"),
			Map.entry("typeorm", "TypeORM"),
			Map.entry("typescript", "TypeScript"),
			Map.entry("vite", "Vite"),
			Map.entry("vitest", "Vitest"),
			Map.entry("webhook", "Webhooks")
	);

	private static final String REPOSITORIES_QUERY = """
			query PortfolioRepositories($cursor: String) {
			  viewer {
			    repositories(
			      first: 100
			      after: $cursor
			      affiliations: [OWNER, COLLABORATOR, ORGANIZATION_MEMBER]
			      orderBy: {field: PUSHED_AT, direction: DESC}
			    ) {
			      nodes {
			        id
			        name
			        nameWithOwner
			        description
			        isPrivate
			        isFork
			        url
			        pushedAt
			        primaryLanguage { name }
			        refs(refPrefix: "refs/heads/", first: 100) {
			          nodes {
			            target {
			              ... on Commit { committedDate }
			            }
			          }
			        }
			      }
			      pageInfo { hasNextPage endCursor }
			    }
			  }
			}
			""";

	private final RestClient restClient;
	private final String token;
	private final Clock clock;
	private final Duration cacheTtl;
	private volatile CacheEntry cache;

	@Autowired
	GithubRepositoryService(
			@Value("${github.api-token:}") String token,
			@Value("${github.api-url:https://api.github.com}") String apiUrl,
			@Value("${github.cache-ttl:PT6H}") Duration cacheTtl
	) {
		this(RestClient.builder(), token, apiUrl, cacheTtl, Clock.systemUTC());
	}

	GithubRepositoryService(
			RestClient.Builder restClientBuilder,
			String token,
			String apiUrl,
			Duration cacheTtl,
			Clock clock
	) {
		this.restClient = restClientBuilder
				.baseUrl(apiUrl)
				.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
				.defaultHeader(HttpHeaders.USER_AGENT, "zorionten-portfolio")
				.build();
		this.token = token;
		this.clock = clock;
		this.cacheTtl = cacheTtl;
	}

	GithubPortfolio getPortfolio() {
		return getData().portfolio();
	}

	GithubKnowledge getKnowledge() {
		return getData().knowledge();
	}

	private GithubData getData() {
		Instant now = clock.instant();
		CacheEntry current = cache;
		if (current != null && now.isBefore(current.expiresAt())) {
			return current.data();
		}

		synchronized (this) {
			current = cache;
			if (current != null && now.isBefore(current.expiresAt())) {
				return current.data();
			}

			GithubData data = fetchData(now);
			cache = new CacheEntry(data, now.plus(cacheTtl));
			return data;
		}
	}

	private GithubData fetchData(Instant fetchedAt) {
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"GitHub repository data is not configured"
			);
		}

		List<GithubPortfolio.Repository> repositories = new ArrayList<>();
		List<RepositoryNode> repositoryNodes = new ArrayList<>();
		String cursor = null;
		boolean hasNextPage;

		do {
			GraphqlResponse response;
			try {
				response = restClient.post()
						.uri("/graphql")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.body(new GraphqlRequest(REPOSITORIES_QUERY, Collections.singletonMap("cursor", cursor)))
						.retrieve()
						.body(GraphqlResponse.class);
			} catch (RuntimeException exception) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub repository data is unavailable", exception);
			}

			if (response == null || response.data() == null || response.data().viewer() == null
					|| response.data().viewer().repositories() == null
					|| response.errors() != null && !response.errors().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub returned an invalid repository response");
			}

			RepositoryConnection connection = response.data().viewer().repositories();
			for (RepositoryNode node : connection.nodes()) {
				repositoryNodes.add(node);
				repositories.add(toRepository(node));
			}

			hasNextPage = connection.pageInfo().hasNextPage();
			cursor = connection.pageInfo().endCursor();
		} while (hasNextPage);

		repositories.sort(Comparator.comparing(GithubPortfolio.Repository::lastUpdatedAt).reversed());
		List<GithubPortfolio.Repository> rushServeRepositories = repositories.stream()
				.filter(repository -> normalize(repository.name()).contains("rushserve"))
				.toList();
		Instant rushServeUpdatedAt = rushServeRepositories.stream()
				.map(GithubPortfolio.Repository::lastUpdatedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);

		GithubPortfolio portfolio = new GithubPortfolio(
				repositories.stream().filter(repository -> !repository.isPrivate()).toList(),
				new GithubPortfolio.RushServeActivity(rushServeRepositories.size(), rushServeUpdatedAt),
				fetchedAt
		);
		return new GithubData(portfolio, buildKnowledge(repositoryNodes, fetchedAt));
	}

	private GithubKnowledge buildKnowledge(List<RepositoryNode> repositories, Instant fetchedAt) {
		List<GithubKnowledge.Source> sources = new ArrayList<>();
		Set<String> privateTechnologies = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		for (RepositoryNode repository : repositories) {
			String readme = loadReadme(repository.nameWithOwner());
			String language = repository.primaryLanguage() == null ? null : repository.primaryLanguage().name();
			if (repository.isPrivate()) {
				privateTechnologies.addAll(inferTechnologies(readme, language));
				continue;
			}

			StringBuilder text = new StringBuilder("Repository: ").append(repository.name());
			if (repository.description() != null && !repository.description().isBlank()) {
				text.append("\nDescription: ").append(repository.description());
			}
			if (language != null) {
				text.append("\nPrimary language: ").append(language);
			}
			if (!readme.isBlank()) {
				text.append("\n\nREADME:\n").append(readme);
			}
			sources.add(new GithubKnowledge.Source(
					"GitHub: " + repository.name(),
					text.toString(),
					repository.url()
			));
		}

		if (!privateTechnologies.isEmpty()) {
			sources.add(new GithubKnowledge.Source(
					"Private project evidence",
					"Private repository metadata and READMEs support hands-on experience with: "
							+ String.join(", ", privateTechnologies)
							+ ". Use this only to infer skills; do not disclose project identities or implementation details.",
					null
			));
		}

		return new GithubKnowledge(List.copyOf(sources), fetchedAt);
	}

	private String loadReadme(String nameWithOwner) {
		String[] parts = nameWithOwner.split("/", 2);
		if (parts.length != 2) {
			return "";
		}

		try {
			String readme = restClient.get()
					.uri("/repos/{owner}/{repository}/readme", parts[0], parts[1])
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header(HttpHeaders.ACCEPT, "application/vnd.github.raw+json")
					.retrieve()
					.body(String.class);
			if (readme == null) {
				return "";
			}
			return readme.substring(0, Math.min(readme.length(), MAX_README_CHARACTERS));
		} catch (RuntimeException ignored) {
			return "";
		}
	}

	private List<String> inferTechnologies(String readme, String language) {
		String evidence = (readme + " " + (language == null ? "" : language)).toLowerCase(Locale.ROOT);
		Set<String> technologies = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		TECHNOLOGIES.forEach((term, displayName) -> {
			Pattern technologyPattern = Pattern.compile(
					"(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])"
			);
			if (technologyPattern.matcher(evidence).find()) {
				technologies.add(displayName);
			}
		});
		if (language != null && !language.isBlank()) {
			technologies.add(language);
		}
		return List.copyOf(technologies);
	}

	private GithubPortfolio.Repository toRepository(RepositoryNode node) {
		Instant latestCommit = node.refs() == null || node.refs().nodes() == null
				? node.pushedAt()
				: node.refs().nodes().stream()
						.filter(ref -> ref.target() != null && ref.target().committedDate() != null)
						.map(ref -> ref.target().committedDate())
						.max(Comparator.naturalOrder())
						.orElse(node.pushedAt());

		return new GithubPortfolio.Repository(
				node.id(),
				node.name(),
				node.description(),
				node.isPrivate(),
				node.isFork(),
				node.primaryLanguage() == null ? null : node.primaryLanguage().name(),
				latestCommit,
				node.isPrivate() ? null : node.url()
		);
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private record GithubData(GithubPortfolio portfolio, GithubKnowledge knowledge) {
	}

	private record CacheEntry(GithubData data, Instant expiresAt) {
	}

	private record GraphqlRequest(String query, Map<String, String> variables) {
	}

	private record GraphqlResponse(GraphqlData data, List<GraphqlError> errors) {
	}

	private record GraphqlData(Viewer viewer) {
	}

	private record Viewer(RepositoryConnection repositories) {
	}

	private record RepositoryConnection(List<RepositoryNode> nodes, PageInfo pageInfo) {
	}

	private record RepositoryNode(
			String id,
			String name,
			String nameWithOwner,
			String description,
			boolean isPrivate,
			boolean isFork,
			String url,
			Instant pushedAt,
			Language primaryLanguage,
			Refs refs
	) {
	}

	private record Language(String name) {
	}

	private record Refs(List<RefNode> nodes) {
	}

	private record RefNode(Commit target) {
	}

	private record Commit(Instant committedDate) {
	}

	private record PageInfo(boolean hasNextPage, String endCursor) {
	}

	private record GraphqlError(String message) {
	}
}
