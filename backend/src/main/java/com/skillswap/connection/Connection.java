package com.skillswap.connection;

import java.time.LocalDateTime;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "connections",
		uniqueConstraints = @UniqueConstraint(name = "uk_connection_pair", columnNames = { "requester_id", "addressee_id" }),
		indexes = { @Index(name = "idx_connections_addressee", columnList = "addressee_id, status"),
				@Index(name = "idx_connections_requester", columnList = "requester_id, status") })
public class Connection extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private User requester;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "addressee_id", nullable = false)
	private User addressee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ConnectionStatus status = ConnectionStatus.PENDING;

	@Column(length = 300)
	private String message;

	@Column(name = "responded_at")
	private LocalDateTime respondedAt;

	public boolean involves(Long userId) {
		return requester.getId().equals(userId) || addressee.getId().equals(userId);
	}

	public User otherParty(Long userId) {
		return requester.getId().equals(userId) ? addressee : requester;
	}
}
