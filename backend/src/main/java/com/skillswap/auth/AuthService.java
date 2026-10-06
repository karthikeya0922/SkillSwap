package com.skillswap.auth;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.auth.dto.AuthResponse;
import com.skillswap.auth.dto.LoginRequest;
import com.skillswap.auth.dto.RegisterRequest;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.common.exception.UnauthorizedException;
import com.skillswap.security.JwtService;
import com.skillswap.security.UserPrincipal;
import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserRegisteredEvent;
import com.skillswap.user.UserRepository;
import com.skillswap.user.UserStatus;
import com.skillswap.user.dto.AuthUserDto;

@Service
public class AuthService {

	private static final String INVALID_CREDENTIALS = "Invalid email or password.";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final LoginAttemptService loginAttempts;
	private final ApplicationEventPublisher events;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			LoginAttemptService loginAttempts, ApplicationEventPublisher events) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.loginAttempts = loginAttempts;
		this.events = events;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("An account with this email already exists.");
		}
		User user = new User();
		user.setFullName(request.fullName().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setCollege(request.college().trim());
		user.setDepartment(request.department().trim());
		user.setYearOfStudy(request.yearOfStudy());
		user.setRole(Role.STUDENT);
		user.setStatus(UserStatus.ACTIVE);
		user.setLastActiveAt(LocalDateTime.now());
		userRepository.save(user);
		events.publishEvent(new UserRegisteredEvent(user.getId()));
		return issue(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		loginAttempts.assertNotLocked(email);
		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			loginAttempts.recordFailure(email);
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
		if (!user.isActive()) {
			throw new ForbiddenException("This account has been suspended."
					+ (user.getSuspensionReason() != null ? " Reason: " + user.getSuspensionReason() : ""));
		}
		loginAttempts.recordSuccess(email);
		user.setLastActiveAt(LocalDateTime.now());
		return issue(user);
	}

	@Transactional(readOnly = true)
	public AuthUserDto me(Long userId) {
		return userRepository.findById(userId).map(AuthUserDto::from)
				.orElseThrow(() -> ResourceNotFoundException.of("User", userId));
	}

	private AuthResponse issue(User user) {
		String token = jwtService.generateToken(UserPrincipal.from(user));
		return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), AuthUserDto.from(user));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
