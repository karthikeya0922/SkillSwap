package com.skillswap.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Human-friendly formatting for notification text. */
final class Formats {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, d MMM 'at' h:mm a", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

	private Formats() {
	}

	static String when(LocalDateTime dateTime) {
		return WHEN.format(dateTime);
	}

	static String time(LocalDateTime dateTime) {
		return TIME.format(dateTime);
	}

	static String credits(BigDecimal credits) {
		return credits.stripTrailingZeros().toPlainString();
	}
}
