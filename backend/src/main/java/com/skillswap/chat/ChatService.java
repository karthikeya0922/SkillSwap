package com.skillswap.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.chat.dto.ChatDtos.ConversationDto;
import com.skillswap.chat.dto.ChatDtos.MessageDto;
import com.skillswap.chat.dto.ChatDtos.ReadReceiptDto;
import com.skillswap.chat.dto.ChatDtos.TypingDto;
import com.skillswap.common.AfterCommit;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.connection.ConnectionService;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

/** One-to-one chat between accepted connections. Messages persist via REST and are pushed over STOMP. */
@Service
public class ChatService {

	public static final String MESSAGE_QUEUE = "/queue/messages";
	public static final String READ_QUEUE = "/queue/read-receipts";
	public static final String TYPING_QUEUE = "/queue/typing";

	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final ConnectionService connectionService;
	private final NotificationService notificationService;
	private final PresenceService presenceService;
	private final SimpMessagingTemplate messaging;

	public ChatService(MessageRepository messageRepository, UserRepository userRepository,
			ConnectionService connectionService, NotificationService notificationService,
			PresenceService presenceService, SimpMessagingTemplate messaging) {
		this.messageRepository = messageRepository;
		this.userRepository = userRepository;
		this.connectionService = connectionService;
		this.notificationService = notificationService;
		this.presenceService = presenceService;
		this.messaging = messaging;
	}

	@Transactional(readOnly = true)
	public List<ConversationDto> conversations(Long meId) {
		Set<Long> latestIds = new HashSet<>(messageRepository.findLatestSentPerPartner(meId));
		latestIds.addAll(messageRepository.findLatestReceivedPerPartner(meId));
		if (latestIds.isEmpty()) {
			return List.of();
		}
		Map<Long, Message> latestByPartner = new HashMap<>();
		for (Message m : messageRepository.findWithParticipants(latestIds)) {
			Long partner = m.getSender().getId().equals(meId) ? m.getRecipient().getId() : m.getSender().getId();
			latestByPartner.merge(partner, m, (a, b) -> a.getId() > b.getId() ? a : b);
		}
		Map<Long, Long> unread = new HashMap<>();
		messageRepository.countUnreadBySender(meId).forEach(row -> unread.put((Long) row[0], (Long) row[1]));
		Set<Long> connected = new HashSet<>(connectionService.connectedUserIds(meId));

		List<ConversationDto> result = new ArrayList<>();
		for (Map.Entry<Long, Message> entry : latestByPartner.entrySet()) {
			Message m = entry.getValue();
			User partner = m.getSender().getId().equals(meId) ? m.getRecipient() : m.getSender();
			result.add(new ConversationDto(UserSummaryDto.from(partner), toDto(m), unread.getOrDefault(entry.getKey(), 0L),
					presenceService.isOnline(partner.getId()), connected.contains(partner.getId())));
		}
		result.sort(Comparator.comparing((ConversationDto c) -> c.lastMessage().id()).reversed());
		return result;
	}

	/** Newest {@code size} messages older than {@code beforeId}, returned oldest-first for display. */
	@Transactional(readOnly = true)
	public List<MessageDto> history(Long meId, Long otherId, Long beforeId, int size) {
		requireUser(otherId);
		requireConnected(meId, otherId);
		List<Message> page = new ArrayList<>(messageRepository.findConversation(meId, otherId, beforeId,
				PageRequest.of(0, Math.min(100, Math.max(1, size)))));
		page.sort(Comparator.comparing(Message::getId));
		return page.stream().map(ChatService::toDto).toList();
	}

	@Transactional
	public MessageDto send(Long meId, Long otherId, String content) {
		User recipient = requireUser(otherId);
		requireConnected(meId, otherId);
		User sender = userRepository.findById(meId).orElseThrow();
		Message message = new Message();
		message.setSender(sender);
		message.setRecipient(recipient);
		message.setContent(content.trim());
		messageRepository.save(message);
		MessageDto dto = toDto(message);
		String recipientKey = recipient.getEmail();
		String senderKey = sender.getEmail();
		AfterCommit.run(() -> {
			messaging.convertAndSendToUser(recipientKey, MESSAGE_QUEUE, dto);
			messaging.convertAndSendToUser(senderKey, MESSAGE_QUEUE, dto);
		});
		if (!presenceService.isOnline(otherId)
				&& !notificationService.hasUnread(otherId, meId, NotificationType.NEW_MESSAGE)) {
			notificationService.notify(otherId, NotificationType.NEW_MESSAGE, "New message from " + sender.getFirstName(),
					preview(content), "/messages/" + meId, meId);
		}
		return dto;
	}

	@Transactional
	public int markRead(Long meId, Long otherId) {
		User other = requireUser(otherId);
		LocalDateTime now = LocalDateTime.now();
		int updated = messageRepository.markRead(otherId, meId, now);
		if (updated > 0) {
			String otherKey = other.getEmail();
			ReadReceiptDto receipt = new ReadReceiptDto(meId, now);
			AfterCommit.run(() -> messaging.convertAndSendToUser(otherKey, READ_QUEUE, receipt));
		}
		return updated;
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long meId) {
		return messageRepository.countByRecipientIdAndReadAtIsNull(meId);
	}

	@Transactional(readOnly = true)
	public void relayTyping(Long meId, Long recipientId, boolean typing) {
		if (recipientId == null || !connectionService.areConnected(meId, recipientId)) {
			return;
		}
		userRepository.findById(recipientId).ifPresent(recipient -> messaging.convertAndSendToUser(
				recipient.getEmail(), TYPING_QUEUE, new TypingDto(meId, recipientId, typing)));
	}

	private void requireConnected(Long meId, Long otherId) {
		if (!connectionService.areConnected(meId, otherId)) {
			throw new ForbiddenException("You can only chat with your connections. Send a connection request first.");
		}
	}

	private User requireUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
	}

	private static String preview(String content) {
		String trimmed = content.trim();
		return trimmed.length() > 120 ? trimmed.substring(0, 117) + "..." : trimmed;
	}

	static MessageDto toDto(Message m) {
		return new MessageDto(m.getId(), m.getSender().getId(), m.getRecipient().getId(),
				m.isRemoved() ? "This message was removed by a moderator." : m.getContent(), m.getCreatedAt(),
				m.getReadAt(), m.isRemoved());
	}
}
