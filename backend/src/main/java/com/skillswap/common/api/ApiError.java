package com.skillswap.common.api;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Envelope used for every error response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(boolean success, int status, String message, Map<String, String> errors, String path,
		Instant timestamp) {

	public static ApiError of(int status, String message, String path) {
		return new ApiError(false, status, message, null, path, Instant.now());
	}

	public static ApiError of(int status, String message, Map<String, String> errors, String path) {
		return new ApiError(false, status, message, errors, path, Instant.now());
	}
}
