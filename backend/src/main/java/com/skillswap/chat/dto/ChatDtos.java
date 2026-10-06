package com.skillswap.chat.dto;

import java.time.LocalDateTime;

import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChatDtos {

	private ChatDtos() {
	}

	public record MessageDto(Long id, Long senderId, Long recipientId, String content, LocalDateTime createdAt,
			LocalDateTime readAt, boolean removed) {
	}

	public record ConversationDto(UserSummaryDto partner, MessageDto lastMessage, long unreadCount, boolean online,
			boolean connected) {
	}

	public record SendMessageRequest(
			@NotBlank(message = "Message cannot be empty") @Size(max = 2000, message = "Messages can be at most 2000 characters") String content) {
	}

	/** Pushed to the other participant when messages are read. */
	public record ReadReceiptDto(Long readerId, LocalDateTime readAt) {
	}

	/** STOMP payload sent by a client while typing, and relayed to the recipient. */
	public record TypingDto(Long userId, Long recipientId, boolean typing) {
	}
}
