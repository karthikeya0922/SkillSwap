package com.skillswap.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.admin.dto.AdminDtos.AdminUserDto;
import com.skillswap.admin.dto.AdminDtos.AnalyticsDto;
import com.skillswap.admin.dto.AdminDtos.SuspendRequest;
import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.report.ModerationService;
import com.skillswap.report.ReportService;
import com.skillswap.report.ReportStatus;
import com.skillswap.report.ReportTargetType;
import com.skillswap.report.dto.ReportDtos.ReportDto;
import com.skillswap.report.dto.ReportDtos.ReviewReportRequest;
import com.skillswap.security.UserPrincipal;
import com.skillswap.skill.SkillService;
import com.skillswap.skill.dto.SkillDtos.SkillDto;
import com.skillswap.user.Role;
import com.skillswap.user.UserStatus;

import jakarta.validation.Valid;

/** All endpoints here require ROLE_ADMIN (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminUserService userModeration;
	private final AnalyticsService analyticsService;
	private final ReportService reportService;
	private final ModerationService moderationService;
	private final SkillService skillService;

	public AdminController(AdminUserService userModeration,
			AnalyticsService analyticsService, ReportService reportService, ModerationService moderationService,
			SkillService skillService) {
		this.userModeration = userModeration;
		this.analyticsService = analyticsService;
		this.reportService = reportService;
		this.moderationService = moderationService;
		this.skillService = skillService;
	}

	@GetMapping("/analytics")
	public ApiResponse<AnalyticsDto> analytics() {
		return ApiResponse.ok(analyticsService.analytics());
	}

	@GetMapping("/users")
	public ApiResponse<PageResponse<AdminUserDto>> users(@RequestParam(required = false) String q,
			@RequestParam(required = false) UserStatus status, @RequestParam(required = false) Role role,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(userModeration.search(q, status, role, page, size));
	}

	@PutMapping("/users/{id}/suspend")
	public ApiResponse<AdminUserDto> suspend(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody(required = false) SuspendRequest request) {
		return ApiResponse.ok(AdminUserDto.from(userModeration.suspend(me.getId(), id,
				request == null ? null : request.reason())), "User suspended");
	}

	@PutMapping("/users/{id}/reactivate")
	public ApiResponse<AdminUserDto> reactivate(@PathVariable Long id) {
		return ApiResponse.ok(AdminUserDto.from(userModeration.reactivate(id)), "User reactivated");
	}

	@GetMapping("/skills")
	public ApiResponse<PageResponse<SkillDto>> skills(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(skillService.adminSearch(q, categoryId, page, size));
	}

	@GetMapping("/reports")
	public ApiResponse<PageResponse<ReportDto>> reports(@RequestParam(required = false) ReportStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(reportService.list(status, page, size));
	}

	@PutMapping("/reports/{id}")
	public ApiResponse<ReportDto> review(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody ReviewReportRequest request) {
		return ApiResponse.ok(reportService.review(me.getId(), id, request), "Report updated");
	}

	@DeleteMapping("/content/{type}/{id}")
	public ApiResponse<Void> removeContent(@PathVariable ReportTargetType type, @PathVariable Long id) {
		moderationService.remove(type, id);
		return ApiResponse.message("Content removed");
	}
}
