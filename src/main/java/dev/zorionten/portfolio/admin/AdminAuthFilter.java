package dev.zorionten.portfolio.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
class AdminAuthFilter extends OncePerRequestFilter {

	static final String ADMIN_KEY_HEADER = "X-Admin-Key";
	private static final String ADMIN_KEY_SHA256 = "e78dcd2ab23bdc60f1f97aced1140b43d2cc47daee343654dc53e8672c74b1f5";

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/admin/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String providedKey = request.getHeader(ADMIN_KEY_HEADER);
		if (providedKey == null || !constantTimeEquals(providedKey.trim(), ADMIN_KEY_SHA256)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Unauthorized\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static boolean constantTimeEquals(String provided, String expected) {
		return MessageDigest.isEqual(
				provided.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				expected.getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);
	}
}