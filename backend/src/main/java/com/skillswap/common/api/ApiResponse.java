package com.skillswap.common.api;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Envelope used for every successful API response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, int status, String message, T data, Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, 200, null, data, Instant.now());
	}

	public static <T> ApiResponse<T> ok(T data, String message) {
		return new ApiResponse<>(true, 200, message, data, Instant.now());
	}

	public static <T> ApiResponse<T> created(T data, String message) {
		return new ApiResponse<>(true, 201, message, data, Instant.now());
	}

	public static ApiResponse<Void> message(String message) {
		return new ApiResponse<>(true, 200, message, null, Instant.now());
	}
}
