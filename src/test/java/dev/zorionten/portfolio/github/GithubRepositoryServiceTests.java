package dev.zorionten.portfolio.github;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubRepositoryServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");

	@Test
	void aggregatesRushServeActivityAndCachesRepositoryData() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.github.test/graphql"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-token"))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "viewer": {
						      "repositories": {
						        "nodes": [
						          {
						            "id": "R_rush_ui",
						            "name": "rush-serve-ui",
						            "nameWithOwner": "team/rush-serve-ui",
						            "description": "RushServe frontend",
						            "isPrivate": true,
						            "isFork": false,
						            "url": "https://github.com/team/rush-serve-ui",
						            "pushedAt": "2026-08-13T00:00:00Z",
						            "primaryLanguage": {"name": "TypeScript"},
						            "refs": {"nodes": [
						              {"target": {"committedDate": "2026-08-11T10:00:00Z"}},
						              {"target": {"committedDate": "2026-08-12T10:00:00Z"}}
						            ]}
						          },
						          {
						            "id": "R_portfolio",
						            "name": "portfolio",
						            "nameWithOwner": "ZorionTen/portfolio",
						            "description": "Portfolio orchestration",
						            "isPrivate": false,
						            "isFork": false,
						            "url": "https://github.com/ZorionTen/portfolio",
						            "pushedAt": "2026-08-14T07:00:00Z",
						            "primaryLanguage": null,
						            "refs": {"nodes": [
						              {"target": {"committedDate": "2026-08-14T07:30:00Z"}}
						            ]}
						          },
						          {
						            "id": "R_rush_docker",
						            "name": "rushserve-docker",
						            "nameWithOwner": "ZorionTen/rushserve-docker",
						            "description": "RushServe local stack",
						            "isPrivate": true,
						            "isFork": false,
						            "url": "https://github.com/ZorionTen/rushserve-docker",
						            "pushedAt": "2026-08-10T00:00:00Z",
						            "primaryLanguage": {"name": "Shell"},
						            "refs": {"nodes": [
						              {"target": {"committedDate": "2026-08-10T08:00:00Z"}}
						            ]}
						          }
						        ],
						        "pageInfo": {"hasNextPage": false, "endCursor": null}
						      }
						    }
						  }
						}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.github.test/repos/team/rush-serve-ui/readme"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("# App\nReact TypeScript frontend", MediaType.TEXT_PLAIN));
		server.expect(requestTo("https://api.github.test/repos/ZorionTen/portfolio/readme"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("# Portfolio\nSpring Boot and FastAPI services", MediaType.TEXT_PLAIN));
		server.expect(requestTo("https://api.github.test/repos/ZorionTen/rushserve-docker/readme"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("# Local stack\nDocker Compose", MediaType.TEXT_PLAIN));

		GithubRepositoryService service = new GithubRepositoryService(
				builder,
				"test-token",
				"https://api.github.test",
				Duration.ofHours(6),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		GithubPortfolio first = service.getPortfolio();
		GithubPortfolio cached = service.getPortfolio();
		GithubKnowledge knowledge = service.getKnowledge();

		assertThat(cached).isSameAs(first);
		assertThat(first.fetchedAt()).isEqualTo(NOW);
		assertThat(first.repositories()).extracting(GithubPortfolio.Repository::name)
				.containsExactly("portfolio");
		assertThat(first.repositories().get(0).url()).isEqualTo("https://github.com/ZorionTen/portfolio");
		assertThat(first.repositories()).allMatch(repository -> !repository.isPrivate());
		assertThat(first.rushServe().repositoryCount()).isEqualTo(2);
		assertThat(first.rushServe().lastUpdatedAt())
				.isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
		assertThat(knowledge.sources()).extracting(GithubKnowledge.Source::source)
				.containsExactly("GitHub: portfolio", "Private project evidence");
		assertThat(knowledge.sources().get(1).text())
				.contains("Docker Compose", "React", "TypeScript")
				.doesNotContain("rush-serve-ui", "rushserve-docker");
		server.verify();
	}

	@Test
	void rejectsRequestsWhenTheGithubTokenIsMissing() {
		GithubRepositoryService service = new GithubRepositoryService(
				RestClient.builder(),
				"",
				"https://api.github.test",
				Duration.ofHours(6),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		assertThatThrownBy(service::getPortfolio)
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("503 SERVICE_UNAVAILABLE");
	}
}
