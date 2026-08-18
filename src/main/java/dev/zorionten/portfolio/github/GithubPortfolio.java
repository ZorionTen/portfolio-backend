package dev.zorionten.portfolio.github;

import java.time.Instant;
import java.util.List;

record GithubPortfolio(
		List<Repository> repositories,
		RushServeActivity rushServe,
		Instant fetchedAt
) {
	record Repository(
			String id,
			String name,
			String description,
			boolean isPrivate,
			boolean isFork,
			List<String> languages,
			Instant lastUpdatedAt,
			String url
	) {
	}

	record RushServeActivity(int repositoryCount, Instant lastUpdatedAt) {
	}
}
