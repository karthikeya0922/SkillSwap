package com.skillswap.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	@Autowired
	private MockMvc mvc;

	private static String registration(String email) {
		return """
				{"fullName":"Test Student","email":"%s","password":"secret123","college":"CBIT",
				 "department":"Computer Science","yearOfStudy":3}
				""".formatted(email);
	}

	@Test
	void registerThenUseTheTokenOnProtectedEndpoints() throws Exception {
		String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registration("flow@test.dev")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.user.email").value("flow@test.dev"))
				.andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.data.token");

		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Test Student"));

		// New accounts get starter credits through the ledger.
		mvc.perform(get("/api/wallet/balance").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.balance").value(5.0));
	}

	@Test
	void duplicateEmailIsRejected() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registration("dupe@test.dev"))).andExpect(status().isCreated());
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registration("DUPE@test.dev")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("An account with this email already exists."));
	}

	@Test
	void invalidRegistrationReturnsFieldErrors() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"A\",\"email\":\"nope\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.college").exists());
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registration("login@test.dev"))).andExpect(status().isCreated());
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"login@test.dev\",\"password\":\"wrongpass1\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void protectedEndpointsRequireAToken() throws Exception {
		mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
		mvc.perform(get("/api/dashboard").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void studentsCannotReachAdminEndpoints() throws Exception {
		String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registration("student@test.dev"))).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.data.token");
		mvc.perform(get("/api/admin/analytics").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}
}
