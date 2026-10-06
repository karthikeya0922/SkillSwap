package com.skillswap.chat;

import java.time.LocalDateTime;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "messages", indexes = { @Index(name = "idx_messages_pair", columnList = "sender_id, recipient_id, id"),
		@Index(name = "idx_messages_unread", columnList = "recipient_id, read_at") })
public class Message extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@Column(nullable = false, length = 2000)
	private String content;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	/** Set when an admin removes the message after a report; content is blanked. */
	@Column(nullable = false)
	private boolean removed;
}
