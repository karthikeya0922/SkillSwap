package com.skillswap.connection.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.skillswap.connection.ConnectionStatus;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ConnectionDtos {

	private ConnectionDtos() {
	}

	/** Relationship between the viewer and another user, from the viewer's side. */
	public enum ConnectionState {
		NONE, PENDING_SENT, PENDING_RECEIVED, CONNECTED, SELF
	}

	public record ConnectionStateDto(ConnectionState state, Long connectionId) {

		public static final ConnectionStateDto NONE = new ConnectionStateDto(ConnectionState.NONE, null);
	}

	public record ConnectionDto(Long id, UserSummaryDto user, ConnectionStatus status, String message,
			boolean incoming, boolean online, LocalDateTime createdAt, LocalDateTime respondedAt) {
	}

	public record ConnectionsOverviewDto(List<ConnectionDto> connections, List<ConnectionDto> incoming,
			List<ConnectionDto> outgoing) {
	}

	public record SendConnectionRequest(@NotNull(message = "Choose who to connect with") Long userId,
			@Size(max = 300, message = "Message can be at most 300 characters") String message) {
	}

	public record RespondConnectionRequest(@NotNull(message = "Choose ACCEPT or REJECT") ConnectionAction action) {
	}

	public enum ConnectionAction {
		ACCEPT, REJECT
	}
}
