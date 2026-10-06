package com.skillswap.connection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.chat.PresenceService;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionDto;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionState;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionStateDto;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionsOverviewDto;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.reputation.ReputationService;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

@Service
public class ConnectionService {

	private final ConnectionRepository connectionRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final ReputationService reputationService;
	private final PresenceService presenceService;

	public ConnectionService(ConnectionRepository connectionRepository, UserRepository userRepository,
			NotificationService notificationService, ReputationService reputationService,
			PresenceService presenceService) {
		this.connectionRepository = connectionRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.reputationService = reputationService;
		this.presenceService = presenceService;
	}

	@Transactional
	public ConnectionDto send(Long meId, Long targetId, String message) {
		if (meId.equals(targetId)) {
			throw new BadRequestException("You cannot connect with yourself.");
		}
		User target = userRepository.findById(targetId).orElseThrow(() -> ResourceNotFoundException.of("User", targetId));
		if (!target.isActive() || target.isAdmin()) {
			throw new BadRequestException("This user is not available for connections.");
		}
		Connection existing = connectionRepository.findBetween(meId, targetId).orElse(null);
		if (existing != null) {
			switch (existing.getStatus()) {
				case ACCEPTED -> throw new ConflictException("You are already connected with " + target.getFullName() + ".");
				case PENDING -> throw new ConflictException(existing.getRequester().getId().equals(meId)
						? "Your connection request is still pending."
						: target.getFullName() + " already sent you a request. Accept it from your connections page.");
				case REJECTED -> {
					connectionRepository.delete(existing);
					connectionRepository.flush();
				}
			}
		}
		User me = userRepository.getReferenceById(meId);
		Connection connection = new Connection();
		connection.setRequester(me);
		connection.setAddressee(target);
		connection.setStatus(ConnectionStatus.PENDING);
		connection.setMessage(message == null || message.isBlank() ? null : message.trim());
		connectionRepository.save(connection);
		notificationService.notify(targetId, NotificationType.CONNECTION_REQUEST, "New connection request",
				me.getFullName() + " wants to connect with you.", "/connections", meId);
		return toDto(connection, meId);
	}

	@Transactional
	public ConnectionDto respond(Long meId, Long connectionId, boolean accept) {
		Connection connection = get(connectionId);
		if (!connection.getAddressee().getId().equals(meId)) {
			throw new ForbiddenException("Only the person who received this request can respond to it.");
		}
		if (connection.getStatus() != ConnectionStatus.PENDING) {
			throw new ConflictException("This request has already been answered.");
		}
		connection.setStatus(accept ? ConnectionStatus.ACCEPTED : ConnectionStatus.REJECTED);
		connection.setRespondedAt(LocalDateTime.now());
		if (accept) {
			User me = connection.getAddressee();
			Long requesterId = connection.getRequester().getId();
			notificationService.notify(requesterId, NotificationType.CONNECTION_ACCEPTED, "Connection accepted",
					me.getFullName() + " accepted your connection request. Say hello!", "/messages/" + meId, meId);
			reputationService.evaluateBadges(meId);
			reputationService.evaluateBadges(requesterId);
		}
		return toDto(connection, meId);
	}

	/** Cancels a pending request or removes an existing connection. */
	@Transactional
	public void remove(Long meId, Long connectionId) {
		Connection connection = get(connectionId);
		if (!connection.involves(meId)) {
			throw new ForbiddenException("This connection does not belong to you.");
		}
		connectionRepository.delete(connection);
	}

	@Transactional(readOnly = true)
	public ConnectionsOverviewDto overview(Long meId) {
		return new ConnectionsOverviewDto(
				connectionRepository.findForUser(meId, ConnectionStatus.ACCEPTED).stream().map(c -> toDto(c, meId)).toList(),
				connectionRepository.findIncomingPending(meId).stream().map(c -> toDto(c, meId)).toList(),
				connectionRepository.findOutgoingPending(meId).stream().map(c -> toDto(c, meId)).toList());
	}

	@Transactional(readOnly = true)
	public boolean areConnected(Long a, Long b) {
		return connectionRepository.findBetween(a, b).map(c -> c.getStatus() == ConnectionStatus.ACCEPTED)
				.orElse(false);
	}

	@Transactional(readOnly = true)
	public ConnectionStateDto stateBetween(Long meId, Long otherId) {
		if (meId.equals(otherId)) {
			return new ConnectionStateDto(ConnectionState.SELF, null);
		}
		return connectionRepository.findBetween(meId, otherId).map(c -> state(c, meId)).orElse(ConnectionStateDto.NONE);
	}

	@Transactional(readOnly = true)
	public Map<Long, ConnectionStateDto> statesFor(Long meId, Collection<Long> otherIds) {
		Map<Long, ConnectionStateDto> result = new HashMap<>();
		if (otherIds.isEmpty()) {
			return result;
		}
		for (Connection c : connectionRepository.findBetweenUserAndOthers(meId, otherIds)) {
			Long other = c.getRequester().getId().equals(meId) ? c.getAddressee().getId() : c.getRequester().getId();
			result.put(other, state(c, meId));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public List<Long> connectedUserIds(Long meId) {
		return connectionRepository.findConnectedUserIds(meId);
	}

	private static ConnectionStateDto state(Connection c, Long meId) {
		ConnectionState state = switch (c.getStatus()) {
			case ACCEPTED -> ConnectionState.CONNECTED;
			case PENDING -> c.getRequester().getId().equals(meId) ? ConnectionState.PENDING_SENT
					: ConnectionState.PENDING_RECEIVED;
			case REJECTED -> ConnectionState.NONE;
		};
		return new ConnectionStateDto(state, state == ConnectionState.NONE ? null : c.getId());
	}

	private Connection get(Long id) {
		return connectionRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Connection", id));
	}

	private ConnectionDto toDto(Connection c, Long meId) {
		User other = c.otherParty(meId);
		return new ConnectionDto(c.getId(), UserSummaryDto.from(other), c.getStatus(), c.getMessage(),
				c.getAddressee().getId().equals(meId), presenceService.isOnline(other.getId()), c.getCreatedAt(),
				c.getRespondedAt());
	}
}
