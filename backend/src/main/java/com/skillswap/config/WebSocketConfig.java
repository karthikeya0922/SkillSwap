package com.skillswap.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.skillswap.security.TokenAuthenticator;

/**
 * STOMP over WebSocket at /ws. The client authenticates in the CONNECT frame with an
 * {@code Authorization: Bearer <jwt>} header; the authenticated principal then keys the user destinations
 * (/user/queue/notifications, /user/queue/messages, ...). Clients may only subscribe to their own user queues.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final TokenAuthenticator tokenAuthenticator;
	private final List<String> allowedOrigins;

	public WebSocketConfig(TokenAuthenticator tokenAuthenticator,
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		this.tokenAuthenticator = tokenAuthenticator;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/queue", "/topic").setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(heartbeatScheduler());
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor == null || accessor.getCommand() == null) {
					return message;
				}
				StompCommand command = accessor.getCommand();
				if (command == StompCommand.CONNECT) {
					String token = TokenAuthenticator.stripBearer(accessor.getFirstNativeHeader("Authorization"));
					accessor.setUser(tokenAuthenticator.authenticate(token)
							.orElseThrow(() -> new MessageDeliveryException("Invalid or expired token")));
				}
				else if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
					if (accessor.getUser() == null) {
						throw new MessageDeliveryException("Not authenticated");
					}
					String destination = accessor.getDestination();
					if (command == StompCommand.SUBSCRIBE && destination != null
							&& !destination.startsWith("/user/queue/")) {
						throw new MessageDeliveryException("Subscriptions are limited to your own queues");
					}
				}
				return message;
			}
		});
	}

	private org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler heartbeatScheduler() {
		org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.initialize();
		return scheduler;
	}
}
