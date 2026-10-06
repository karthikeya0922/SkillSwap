package com.skillswap.common;

import java.time.Instant;

/** Minimal JSON writing for the few places that respond outside Spring MVC (security filters). */
public final class Json {

	private Json() {
	}

	public static String quote(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
		for (char c : value.toCharArray()) {
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					}
					else {
						sb.append(c);
					}
				}
			}
		}
		return sb.append('"').toString();
	}

	public static String error(int status, String message, String path) {
		return "{\"success\":false,\"status\":" + status + ",\"message\":" + quote(message) + ",\"path\":"
				+ quote(path) + ",\"timestamp\":" + quote(Instant.now().toString()) + "}";
	}
}
