package com.skillswap.match;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.match.dto.MatchDto;
import com.skillswap.security.UserPrincipal;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

	private final MatchQueryService queries;

	public MatchController(MatchQueryService queries) {
		this.queries = queries;
	}

	@GetMapping
	public ApiResponse<PageResponse<MatchDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "0") int minScore, @RequestParam(defaultValue = "false") boolean mutualOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(queries.list(me.getId(), minScore, mutualOnly, page, size));
	}

	@GetMapping("/recommended")
	public ApiResponse<List<MatchDto>> recommended(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "5") int limit) {
		return ApiResponse.ok(queries.recommended(me.getId(), limit));
	}

	@GetMapping("/{userId}")
	public ApiResponse<MatchDto> with(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long userId) {
		return ApiResponse.ok(queries.with(me.getId(), userId));
	}
}
