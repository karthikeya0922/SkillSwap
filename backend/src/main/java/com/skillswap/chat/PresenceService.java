package com.skillswap.chat;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.skillswap.connection.ConnectionRepository;
import com.skillswap.security.UserPrincipal;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/**
 * Tracks which users have at least one open WebSocket session and tells their connections when they come online or
 * go offline. State is in-memory, which is fine for a single backend instance.
 */
@Service
public class PresenceService {

	public record PresenceDto(Long userId, boolean online) {
	}

	private final Map<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
	private final ConnectionRepository connectionRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messaging;

	public PresenceService(ConnectionRepository connectionRepository, UserRepository userRepository,
			SimpMessagingTemplate messaging) {
		this.connectionRepository = connectionRepository;
		this.userRepository = userRepository;
		this.messaging = messaging;
	}

	public boolean isOnline(Long userId) {
		Set<String> sessions = sessionsByUser.get(userId);
		return sessions != null && !sessions.isEmpty();
	}

	public Set<Long> onlineAmong(Collection<Long> userIds) {
		return userIds.stream().filter(this::isOnline).collect(Collectors.toSet());
	}

	@EventListener
	@Transactional
	public void onConnected(SessionConnectedEvent event) {
		Long userId = userId(event.getUser());
		String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
		if (userId == null || sessionId == null) {
			return;
		}
		Set<String> sessions = sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
		boolean cameOnline = sessions.isEmpty();
		sessions.add(sessionId);
		touch(userId);
		if (cameOnline) {
			broadcast(userId, true);
		}
	}

	@EventListener
	@Transactional
	public void onDisconnected(SessionDisconnectEvent event) {
		Long userId = userId(event.getUser());
		if (userId == null) {
			return;
		}
		Set<String> sessions = sessionsByUser.get(userId);
		if (sessions == null) {
			return;
		}
		sessions.remove(event.getSessionId());
		if (sessions.isEmpty()) {
			sessionsByUser.remove(userId);
			touch(userId);
			broadcast(userId, false);
		}
	}

	private void touch(Long userId) {
		userRepository.findById(userId).ifPresent(u -> u.setLastActiveAt(LocalDateTime.now()));
	}

	private void broadcast(Long userId, boolean online) {
		List<Long> connectionIds = connectionRepository.findConnectedUserIds(userId);
		if (connectionIds.isEmpty()) {
			return;
		}
		PresenceDto payload = new PresenceDto(userId, online);
		for (User other : userRepository.findAllById(connectionIds)) {
			if (isOnline(other.getId())) {
				messaging.convertAndSendToUser(other.getEmail(), "/queue/presence", payload);
			}
		}
	}

	private static Long userId(Principal principal) {
		if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal user) {
			return user.getId();
		}
		return null;
	}
}
