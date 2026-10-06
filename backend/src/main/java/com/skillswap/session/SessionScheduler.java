package com.skillswap.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionScheduler {

	private static final Logger log = LoggerFactory.getLogger(SessionScheduler.class);

	private final SessionService sessionService;

	public SessionScheduler(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@Scheduled(fixedDelayString = "${app.sessions.timeline-interval-ms:60000}", initialDelay = 15000)
	public void tick() {
		try {
			sessionService.processTimeline();
		}
		catch (RuntimeException ex) {
			log.warn("Session timeline processing failed", ex);
		}
	}
}
