package com.skillswap.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skillswap.common.exception.ApiException;

/** Simple in-memory throttle against password guessing: 5 failures locks an email for 15 minutes. */
@Service
public class LoginAttemptService {

	private static final int MAX_FAILURES = 5;
	private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

	private record Attempts(int failures, Instant firstFailure) {
	}

	private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

	public void assertNotLocked(String email) {
		Attempts current = attempts.get(key(email));
		if (current == null) {
			return;
		}
		if (current.firstFailure().plus(LOCK_WINDOW).isBefore(Instant.now())) {
			attempts.remove(key(email));
			return;
		}
		if (current.failures() >= MAX_FAILURES) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many failed sign-in attempts. Please wait 15 minutes and try again.");
		}
	}

	public void recordFailure(String email) {
		attempts.merge(key(email), new Attempts(1, Instant.now()),
				(old, ignored) -> new Attempts(old.failures() + 1, old.firstFailure()));
	}

	public void recordSuccess(String email) {
		attempts.remove(key(email));
	}

	private String key(String email) {
		return email.trim().toLowerCase();
	}
}
