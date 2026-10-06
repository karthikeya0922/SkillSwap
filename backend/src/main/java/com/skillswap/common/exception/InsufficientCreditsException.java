package com.skillswap.common.exception;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

public class InsufficientCreditsException extends ApiException {

	public InsufficientCreditsException(BigDecimal required, BigDecimal available) {
		super(HttpStatus.valueOf(422), "Not enough skill credits. This needs " + required.stripTrailingZeros().toPlainString()
				+ " credits but the balance is " + available.stripTrailingZeros().toPlainString() + ".");
	}

	public InsufficientCreditsException(String message) {
		super(HttpStatus.valueOf(422), message);
	}
}
