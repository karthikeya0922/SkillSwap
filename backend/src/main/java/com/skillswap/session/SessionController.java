package com.skillswap.session;

import org.springframework.http.HttpStatus;
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

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.security.UserPrincipal;
import com.skillswap.session.dto.SessionDtos.BookSessionRequest;
import com.skillswap.session.dto.SessionDtos.ReasonRequest;
import com.skillswap.session.dto.SessionDtos.SessionDetailsRequest;
import com.skillswap.session.dto.SessionDtos.SessionDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

	private final SessionService sessionService;

	public SessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SessionDto> book(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody BookSessionRequest request) {
		return ApiResponse.created(sessionService.book(me.getId(), request), "Session requested");
	}

	@GetMapping
	public ApiResponse<PageResponse<SessionDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "ALL") String role, @RequestParam(defaultValue = "upcoming") String scope,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(sessionService.list(me.getId(), role, scope, page, size));
	}

	@GetMapping("/{id}")
	public ApiResponse<SessionDto> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(sessionService.get(me, id));
	}

	@PutMapping("/{id}")
	public ApiResponse<SessionDto> updateDetails(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody SessionDetailsRequest request) {
		return ApiResponse.ok(sessionService.updateDetails(me.getId(), id, request), "Session details updated");
	}

	@PostMapping("/{id}/accept")
	public ApiResponse<SessionDto> accept(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody(required = false) SessionDetailsRequest request) {
		return ApiResponse.ok(sessionService.accept(me.getId(), id, request), "Session accepted");
	}

	@PostMapping("/{id}/reject")
	public ApiResponse<SessionDto> reject(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody(required = false) ReasonRequest request) {
		return ApiResponse.ok(sessionService.reject(me.getId(), id, request == null ? null : request.reason()),
				"Session declined");
	}

	@PostMapping("/{id}/cancel")
	public ApiResponse<SessionDto> cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody(required = false) ReasonRequest request) {
		return ApiResponse.ok(sessionService.cancel(me.getId(), id, request == null ? null : request.reason()),
				"Session cancelled");
	}

	@PostMapping("/{id}/complete")
	public ApiResponse<SessionDto> complete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(sessionService.complete(me.getId(), id), "Session marked as completed");
	}
}
