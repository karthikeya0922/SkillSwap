package com.skillswap.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Base class for domain errors that map directly onto an HTTP status. */
@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
