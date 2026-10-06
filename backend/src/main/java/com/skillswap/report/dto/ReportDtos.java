package com.skillswap.report.dto;

import java.time.LocalDateTime;

import com.skillswap.report.ReportAction;
import com.skillswap.report.ReportReason;
import com.skillswap.report.ReportStatus;
import com.skillswap.report.ReportTargetType;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReportDtos {

	private ReportDtos() {
	}

	public record CreateReportRequest(
			@NotNull(message = "Choose what you are reporting") ReportTargetType targetType,
			@NotNull(message = "Target is required") Long targetId,
			@NotNull(message = "Choose a reason") ReportReason reason,
			@Size(max = 1000, message = "Details can be at most 1000 characters") String details) {
	}

	public record ReviewReportRequest(
			@NotNull(message = "Choose a status") ReportStatus status,
			ReportAction action,
			@Size(max = 1000, message = "Note can be at most 1000 characters") String adminNote) {
	}

	public record ReportDto(Long id, UserSummaryDto reporter, UserSummaryDto reportedUser, String reportedUserEmail,
			ReportTargetType targetType, Long targetId, String targetPreview, ReportReason reason, String details,
			ReportStatus status, ReportAction actionTaken, String adminNote, String resolvedByName,
			LocalDateTime resolvedAt, LocalDateTime createdAt) {
	}
}
