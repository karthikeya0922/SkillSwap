package com.skillswap.dashboard;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.dashboard.DashboardService.DashboardDto;
import com.skillswap.reputation.ReputationService;
import com.skillswap.reputation.dto.ReputationDtos.BadgeDto;
import com.skillswap.reputation.dto.ReputationDtos.LeaderboardEntryDto;
import com.skillswap.security.UserPrincipal;

@RestController
@RequestMapping("/api")
public class DashboardController {

	private final DashboardService dashboardService;
	private final ReputationService reputationService;

	public DashboardController(DashboardService dashboardService, ReputationService reputationService) {
		this.dashboardService = dashboardService;
		this.reputationService = reputationService;
	}

	@GetMapping("/dashboard")
	public ApiResponse<DashboardDto> dashboard(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(dashboardService.dashboard(me.getId()));
	}

	@GetMapping("/reputation/leaderboard")
	public ApiResponse<List<LeaderboardEntryDto>> leaderboard(@RequestParam(defaultValue = "10") int limit) {
		return ApiResponse.ok(reputationService.leaderboard(limit));
	}

	@GetMapping("/reputation/badges")
	public ApiResponse<List<BadgeDto>> badges() {
		return ApiResponse.ok(reputationService.catalog());
	}
}
