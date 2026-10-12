package com.skillswap;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.skillswap.common.logging.RequestLoggingFilter;
import com.skillswap.security.JwtService;
import com.skillswap.security.UserPrincipal;
import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** API docs, monitoring endpoints and request tracing, including who may see what. */
@SpringBootTest
@AutoConfigureMockMvc
class PlatformEndpointsTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtService jwtService;

	private String tokenFor(Role role) {
		User user = new User();
		user.setFullName("Platform " + role);
		user.setEmail(role.name().toLowerCase() + System.nanoTime() + "@test.dev");
		user.setPasswordHash("not-a-real-hash");
		user.setCollege("CBIT");
		user.setDepartment("CSE");
		user.setYearOfStudy(1);
		user.setRole(role);
		return "Bearer " + jwtService.generateToken(UserPrincipal.from(userRepository.save(user)));
	}

	@Test
	void openApiDocumentDescribesTheApiAndTheJwtScheme() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("SkillSwap API"))
				.andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
				.andExpect(jsonPath("$.paths['/api/sessions']").exists())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void healthIsPublicButMetricsAreAdminOnly() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());

		mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/metrics").header("Authorization", tokenFor(Role.STUDENT)))
				.andExpect(status().isForbidden());

		String admin = tokenFor(Role.ADMIN);
		mvc.perform(get("/actuator/metrics").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.names").isArray());
		mvc.perform(get("/actuator/health").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.db.status").value("UP"));
	}

	@Test
	void apiResponsesCarryARequestIdForLogCorrelation() throws Exception {
		mvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, matchesPattern("[0-9a-f]{8}")));
	}
}
