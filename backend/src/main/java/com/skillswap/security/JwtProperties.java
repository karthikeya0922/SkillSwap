package com.skillswap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JWT settings. The secret must come from the environment (JWT_SECRET), never from source control. */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank(message = "JWT_SECRET must be set") @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
		@Min(5) long expirationMinutes) {
}
