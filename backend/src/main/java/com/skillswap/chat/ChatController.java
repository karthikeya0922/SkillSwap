package com.skillswap.chat;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.chat.dto.ChatDtos.ConversationDto;
import com.skillswap.chat.dto.ChatDtos.MessageDto;
import com.skillswap.chat.dto.ChatDtos.SendMessageRequest;
import com.skillswap.chat.dto.ChatDtos.TypingDto;
import com.skillswap.common.api.ApiResponse;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@GetMapping("/conversations")
	public ApiResponse<List<ConversationDto>> conversations(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(chatService.conversations(me.getId()));
	}

	@GetMapping("/unread-count")
	public ApiResponse<Map<String, Long>> unread(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(Map.of("count", chatService.unreadCount(me.getId())));
	}

	@GetMapping("/{userId}")
	public ApiResponse<List<MessageDto>> history(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long userId,
			@RequestParam(required = false) Long before, @RequestParam(defaultValue = "40") int size) {
		return ApiResponse.ok(chatService.history(me.getId(), userId, before, size));
	}

	@PostMapping("/{userId}")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<MessageDto> send(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long userId,
			@Valid @RequestBody SendMessageRequest request) {
		return ApiResponse.created(chatService.send(me.getId(), userId, request.content()), null);
	}

	@PutMapping("/{userId}/read")
	public ApiResponse<Map<String, Integer>> markRead(@AuthenticationPrincipal UserPrincipal me,
			@PathVariable Long userId) {
		return ApiResponse.ok(Map.of("updated", chatService.markRead(me.getId(), userId)));
	}

	/** STOMP: clients send {recipientId, typing} to /app/chat.typing. */
	@MessageMapping("/chat.typing")
	public void typing(@Payload TypingDto payload, Principal principal) {
		if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal user) {
			chatService.relayTyping(user.getId(), payload.recipientId(), payload.typing());
		}
	}
}
