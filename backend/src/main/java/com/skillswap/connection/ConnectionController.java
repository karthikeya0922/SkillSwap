package com.skillswap.connection;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionAction;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionDto;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionsOverviewDto;
import com.skillswap.connection.dto.ConnectionDtos.RespondConnectionRequest;
import com.skillswap.connection.dto.ConnectionDtos.SendConnectionRequest;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

	private final ConnectionService connectionService;

	public ConnectionController(ConnectionService connectionService) {
		this.connectionService = connectionService;
	}

	@GetMapping
	public ApiResponse<ConnectionsOverviewDto> list(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(connectionService.overview(me.getId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ConnectionDto> send(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody SendConnectionRequest request) {
		return ApiResponse.created(connectionService.send(me.getId(), request.userId(), request.message()),
				"Connection request sent");
	}

	@PutMapping("/{id}")
	public ApiResponse<ConnectionDto> respond(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody RespondConnectionRequest request) {
		boolean accept = request.action() == ConnectionAction.ACCEPT;
		return ApiResponse.ok(connectionService.respond(me.getId(), id, accept),
				accept ? "Connection accepted" : "Request declined");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		connectionService.remove(me.getId(), id);
		return ApiResponse.message("Connection removed");
	}
}
