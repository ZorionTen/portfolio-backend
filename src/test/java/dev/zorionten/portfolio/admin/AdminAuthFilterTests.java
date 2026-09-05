package dev.zorionten.portfolio.admin;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthFilterTests {

	private final AdminAuthFilter filter = new AdminAuthFilter();

	@Test
	void rejectsRequestsWithoutAdminKey() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/sessions");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void rejectsRequestsWithWrongAdminKey() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/sessions");
		request.addHeader(AdminAuthFilter.ADMIN_KEY_HEADER, "wrong-key");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void acceptsRequestsWithCorrectAdminKey() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/sessions");
		request.addHeader(AdminAuthFilter.ADMIN_KEY_HEADER, "e78dcd2ab23bdc60f1f97aced1140b43d2cc47daee343654dc53e8672c74b1f5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	void doesNotFilterNonAdminPaths() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat-messages");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isNotNull();
	}
}