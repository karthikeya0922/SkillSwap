package com.skillswap.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final String ISSUER = "skillswap";

	private final SecretKey key;
	private final long expirationMinutes;

	public JwtService(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = properties.expirationMinutes();
	}

	public String generateToken(UserPrincipal principal) {
		Instant now = Instant.now();
		return Jwts.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(principal.getId()))
				.claim("email", principal.getUsername())
				.claim("role", principal.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}

	public long getExpirationSeconds() {
		return expirationMinutes * 60;
	}

	/** Returns the user id carried by a valid, unexpired token, or empty for anything else. */
	public Optional<Long> extractUserId(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
					.parseSignedClaims(token).getPayload();
			return Optional.of(Long.valueOf(claims.getSubject()));
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
