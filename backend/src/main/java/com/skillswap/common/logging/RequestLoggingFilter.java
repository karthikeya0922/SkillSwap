package com.skillswap.common.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs one line per API call with method, path, status and duration, and tags every log line written while handling
 * the request with a {@code requestId} (in the MDC, so it appears as a field in the structured log file). Calls slower
 * than {@code app.logging.slow-request-ms} are logged as warnings.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID = "requestId";
	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	private final long slowRequestMillis;

	public RequestLoggingFilter(@Value("${app.logging.slow-request-ms:1000}") long slowRequestMillis) {
		this.slowRequestMillis = slowRequestMillis;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = UUID.randomUUID().toString().substring(0, 8);
		long start = System.nanoTime();
		MDC.put(REQUEST_ID, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			long millis = (System.nanoTime() - start) / 1_000_000;
			int status = response.getStatus();
			if (millis >= slowRequestMillis) {
				log.warn("Slow request {} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), status,
						millis);
			}
			else if (status >= 500) {
				log.error("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), status, millis);
			}
			else {
				log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), status, millis);
			}
			MDC.remove(REQUEST_ID);
		}
	}
}
