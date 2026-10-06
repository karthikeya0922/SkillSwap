package com.skillswap.auth.dto;

import com.skillswap.user.dto.AuthUserDto;

public record AuthResponse(String token, String tokenType, long expiresIn, AuthUserDto user) {

	public static AuthResponse bearer(String token, long expiresIn, AuthUserDto user) {
		return new AuthResponse(token, "Bearer", expiresIn, user);
	}
}
