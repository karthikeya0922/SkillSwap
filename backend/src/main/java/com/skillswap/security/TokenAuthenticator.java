package com.skillswap.security;

import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.user.UserRepository;

/**
 * Resolves a bearer token into an authentication. The user is re-read from the database on every call so
 * suspensions and role changes take effect immediately, even for tokens that are still unexpired.
 */
@Component
public class TokenAuthenticator {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public TokenAuthenticator(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Optional<UsernamePasswordAuthenticationToken> authenticate(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return jwtService.extractUserId(token)
				.flatMap(userRepository::findById)
				.filter(user -> user.isActive())
				.map(UserPrincipal::from)
				.map(principal -> new UsernamePasswordAuthenticationToken(principal, null,
						principal.getAuthorities()));
	}

	public static String stripBearer(String header) {
		if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return header.substring(7).trim();
		}
		return null;
	}
}
