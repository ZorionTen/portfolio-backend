package dev.zorionten.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOriginPatterns(
						"https://zorionten.github.io",
						"http://localhost:*",
						"http://127.0.0.1:*"
				)
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("Content-Type", "X-Admin-Key")
				.maxAge(3600);
	}
}
