package com.skillswap.notification;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.notification.NotificationService.NotificationDto;
import com.skillswap.security.UserPrincipal;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public ApiResponse<PageResponse<NotificationDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "false") boolean unreadOnly, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(notificationService.list(me.getId(), unreadOnly, page, size));
	}

	@GetMapping("/unread-count")
	public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(Map.of("count", notificationService.unreadCount(me.getId())));
	}

	@PutMapping("/{id}/read")
	public ApiResponse<Void> markRead(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		notificationService.markRead(me.getId(), id);
		return ApiResponse.message("Notification marked as read");
	}

	@PutMapping("/read-all")
	public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(Map.of("updated", notificationService.markAllRead(me.getId())),
				"All notifications marked as read");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		notificationService.delete(me.getId(), id);
		return ApiResponse.message("Notification deleted");
	}
}
