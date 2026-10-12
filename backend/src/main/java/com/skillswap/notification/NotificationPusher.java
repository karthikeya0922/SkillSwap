package com.skillswap.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.skillswap.notification.NotificationService.NotificationDto;

/**
 * Delivers live notifications over WebSocket on a background thread, so the HTTP request that triggered them never
 * waits on socket I/O. The notification row is already stored; a failed push only means the user sees it on their
 * next refresh instead of instantly.
 */
@Component
public class NotificationPusher {

	private static final Logger log = LoggerFactory.getLogger(NotificationPusher.class);

	private final SimpMessagingTemplate messaging;

	public NotificationPusher(SimpMessagingTemplate messaging) {
		this.messaging = messaging;
	}

	@Async
	public void push(String userEmail, NotificationDto notification) {
		try {
			messaging.convertAndSendToUser(userEmail, NotificationService.USER_QUEUE, notification);
		}
		catch (MessagingException ex) {
			log.warn("Could not push notification {} to its recipient: {}", notification.id(), ex.getMessage());
		}
	}
}
