package com.skillswap.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.auth.dto.AuthResponse;
import com.skillswap.auth.dto.LoginRequest;
import com.skillswap.auth.dto.RegisterRequest;
import com.skillswap.common.api.ApiResponse;
import com.skillswap.security.UserPrincipal;
import com.skillswap.user.dto.AuthUserDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.created(authService.register(request), "Welcome to SkillSwap!");
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request), "Signed in successfully");
	}

	@GetMapping("/me")
	public ApiResponse<AuthUserDto> me(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(authService.me(me.getId()));
	}

	/** Tokens are stateless; the client discards its token. Kept as an endpoint for a uniform client flow. */
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		return ApiResponse.message("Signed out");
	}
}
