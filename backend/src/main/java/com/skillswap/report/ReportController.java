package com.skillswap.report;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.report.dto.ReportDtos.CreateReportRequest;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> report(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody CreateReportRequest request) {
		reportService.create(me.getId(), request);
		return ApiResponse.created(null, "Thanks — our moderators will review this report.");
	}
}
