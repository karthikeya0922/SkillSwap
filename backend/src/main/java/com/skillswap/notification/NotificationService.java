package com.skillswap.notification;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.AfterCommit;
import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

@Service
public class NotificationService {

	public record ActorDto(Long id, String fullName, String avatarUrl) {
	}

	public record NotificationDto(Long id, NotificationType type, String title, String message, String link,
			boolean read, LocalDateTime createdAt, ActorDto actor) {
	}

	public static final String USER_QUEUE = "/queue/notifications";

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messaging;

	public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
			SimpMessagingTemplate messaging) {
		this.notificationRepository = notificationRepository;
		this.userRepository = userRepository;
		this.messaging = messaging;
	}

	/**
	 * Stores a notification and pushes it over WebSocket once the caller's transaction commits. Joins the caller's
	 * transaction so a rolled-back action never leaves a stray notification behind.
	 */
	@Transactional
	public void notify(Long recipientId, NotificationType type, String title, String message, String link,
			Long actorId) {
		User recipient = userRepository.findById(recipientId).orElse(null);
		if (recipient == null) {
			return;
		}
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		notification.setType(type);
		notification.setTitle(truncate(title, 120));
		notification.setMessage(truncate(message, 500));
		notification.setLink(link);
		if (actorId != null) {
			notification.setActor(userRepository.getReferenceById(actorId));
		}
		notificationRepository.save(notification);
		NotificationDto dto = toDto(notification);
		String destinationUser = recipient.getEmail();
		AfterCommit.run(() -> messaging.convertAndSendToUser(destinationUser, USER_QUEUE, dto));
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationDto> list(Long userId, boolean unreadOnly, int page, int size) {
		Page<Notification> result = notificationRepository.findForUser(userId, unreadOnly, PageRequests.of(page, size));
		return PageResponse.from(result, NotificationService::toDto);
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return notificationRepository.countByRecipientIdAndReadFalse(userId);
	}

	@Transactional
	public void markRead(Long userId, Long id) {
		owned(userId, id).setRead(true);
	}

	@Transactional
	public int markAllRead(Long userId) {
		return notificationRepository.markAllRead(userId);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		notificationRepository.delete(owned(userId, id));
	}

	@Transactional(readOnly = true)
	public boolean hasUnread(Long recipientId, Long actorId, NotificationType type) {
		return notificationRepository.existsByRecipientIdAndActorIdAndTypeAndReadFalse(recipientId, actorId, type);
	}

	private Notification owned(Long userId, Long id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
		if (!notification.getRecipient().getId().equals(userId)) {
			throw new ForbiddenException("This notification belongs to someone else.");
		}
		return notification;
	}

	static NotificationDto toDto(Notification n) {
		User actor = n.getActor();
		ActorDto actorDto = actor == null ? null : new ActorDto(actor.getId(), actor.getFullName(), actor.getAvatarUrl());
		return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), n.isRead(),
				n.getCreatedAt(), actorDto);
	}

	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
	}
}
