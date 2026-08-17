package dev.zorionten.portfolio.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.UUID;

public final class PortfolioRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(UUID[].class);
		hints.reflection().registerTypeIfPresent(
				classLoader,
				"org.hibernate.validator.internal.util.logging.Log_$logger",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS
		);
		hints.reflection().registerTypeIfPresent(
				classLoader,
				"org.hibernate.validator.internal.util.logging.Messages_$bundle",
				MemberCategory.INVOKE_PUBLIC_METHODS
		);
		hints.reflection().registerTypeIfPresent(
				classLoader,
				"dev.zorionten.portfolio.github.GithubRepositoryService$GraphqlRequest",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS
		);
	}

}
