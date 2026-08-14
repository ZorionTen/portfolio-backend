package dev.zorionten.portfolio.github;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/github")
class GithubRepositoryController {

	private static final Duration CACHE_TTL = Duration.ofHours(6);
	private final GithubRepositoryService repositoryService;

	GithubRepositoryController(GithubRepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@GetMapping("/repositories")
	ResponseEntity<GithubPortfolio> list() {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(CACHE_TTL).cachePublic())
				.body(repositoryService.getPortfolio());
	}

	@GetMapping("/knowledge")
	ResponseEntity<GithubKnowledge> knowledge() {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(CACHE_TTL).cachePublic())
				.body(repositoryService.getKnowledge());
	}
}
