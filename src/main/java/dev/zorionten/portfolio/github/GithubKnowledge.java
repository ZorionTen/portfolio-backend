package dev.zorionten.portfolio.github;

import java.time.Instant;
import java.util.List;

record GithubKnowledge(List<Source> sources, Instant fetchedAt) {
	record Source(String source, String text, String url) {
	}
}
