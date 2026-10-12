package com.skillswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API documentation served at {@code /swagger-ui.html}. Use the Authorize button with the token returned by
 * {@code POST /api/auth/login} to call protected endpoints.
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	public OpenAPI skillSwapOpenApi() {
		return new OpenAPI()
				.info(new Info().title("SkillSwap API").version("1.0.0")
						.description("Peer-to-peer skill exchange for college students. Every response is wrapped in "
								+ "{ success, status, message, data, timestamp }."))
				.components(new Components().addSecuritySchemes(BEARER,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
