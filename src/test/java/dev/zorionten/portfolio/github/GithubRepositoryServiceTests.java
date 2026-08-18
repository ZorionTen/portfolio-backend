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
		server.expect(requestTo("https://api.github.test/user/repos?sort=pushed&direction=desc&per_page=100&page=1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer test-token"))
				.andRespond(withSuccess("""
						[
						  {
						    "id": 1001,
						    "name": "rush-serve-ui",
						    "full_name": "team/rush-serve-ui",
						    "description": "RushServe frontend",
						    "private": true,
						    "fork": false,
						    "html_url": "https://github.com/team/rush-serve-ui",
						    "pushed_at": "2026-08-13T00:00:00Z",
						    "language": "TypeScript"
						  },
						  {
						    "id": 1002,
						    "name": "portfolio",
						    "full_name": "ZorionTen/portfolio",
						    "description": "Portfolio orchestration",
						    "private": false,
						    "fork": false,
						    "html_url": "https://github.com/ZorionTen/portfolio",
						    "pushed_at": "2026-08-14T07:00:00Z",
						    "language": null
						  },
						  {
						    "id": 1003,
						    "name": "rushserve-docker",
						    "full_name": "ZorionTen/rushserve-docker",
						    "description": "RushServe local stack",
						    "private": true,
						    "fork": false,
						    "html_url": "https://github.com/ZorionTen/rushserve-docker",
						    "pushed_at": "2026-08-10T00:00:00Z",
						    "language": "Shell"
						  }
						]
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
		// Now includes all repos (private + public), private ones have null URL
		assertThat(first.repositories()).extracting(GithubPortfolio.Repository::name)
				.containsExactly("portfolio", "rush-serve-ui", "rushserve-docker");
		// Public repo has URL, private repos have null URL
		assertThat(first.repositories())
				.filteredOn(repository -> !repository.isPrivate())
				.extracting(GithubPortfolio.Repository::url)
				.containsExactly("https://github.com/ZorionTen/portfolio");
		assertThat(first.repositories())
				.filteredOn(GithubPortfolio.Repository::isPrivate)
				.extracting(GithubPortfolio.Repository::url)
				.containsOnlyNulls();
		assertThat(first.rushServe().repositoryCount()).isEqualTo(2);
		assertThat(first.rushServe().lastUpdatedAt())
				.isEqualTo(Instant.parse("2026-08-13T00:00:00Z"));
		// Knowledge includes public repo + private project evidence
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
