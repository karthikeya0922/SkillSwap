package com.skillswap.report;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.admin.AdminUserService;
import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.report.ModerationService.TargetInfo;
import com.skillswap.report.dto.ReportDtos.CreateReportRequest;
import com.skillswap.report.dto.ReportDtos.ReportDto;
import com.skillswap.report.dto.ReportDtos.ReviewReportRequest;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

@Service
public class ReportService {

	private final ReportRepository reportRepository;
	private final UserRepository userRepository;
	private final ModerationService moderationService;
	private final AdminUserService userModerationService;
	private final NotificationService notificationService;

	public ReportService(ReportRepository reportRepository, UserRepository userRepository,
			ModerationService moderationService, AdminUserService userModerationService,
			NotificationService notificationService) {
		this.reportRepository = reportRepository;
		this.userRepository = userRepository;
		this.moderationService = moderationService;
		this.userModerationService = userModerationService;
		this.notificationService = notificationService;
	}

	@Transactional
	public ReportDto create(Long meId, CreateReportRequest request) {
		TargetInfo target = moderationService.resolve(request.targetType(), request.targetId(), meId);
		if (target.owner() != null && target.owner().getId().equals(meId)) {
			throw new BadRequestException("You cannot report your own content.");
		}
		if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(meId, request.targetType(),
				request.targetId(), ReportStatus.OPEN)) {
			throw new ConflictException("You have already reported this. Our team will review it soon.");
		}
		Report report = new Report();
		report.setReporter(userRepository.getReferenceById(meId));
		report.setReportedUser(target.owner());
		report.setTargetType(request.targetType());
		report.setTargetId(request.targetId());
		report.setTargetPreview(truncate(target.preview(), 500));
		report.setReason(request.reason());
		report.setDetails(request.details() == null || request.details().isBlank() ? null : request.details().trim());
		reportRepository.save(report);
		return toDto(report);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReportDto> list(ReportStatus status, int page, int size) {
		return PageResponse.from(reportRepository.search(status, PageRequests.of(page, size)), ReportService::toDto);
	}

	@Transactional
	public ReportDto review(Long adminId, Long id, ReviewReportRequest request) {
		Report report = reportRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Report", id));
		ReportAction action = request.action() == null ? ReportAction.NONE : request.action();
		boolean closing = EnumSet.of(ReportStatus.RESOLVED, ReportStatus.DISMISSED).contains(request.status());
		if (action != ReportAction.NONE && request.status() != ReportStatus.RESOLVED) {
			throw new BadRequestException("Taking an action resolves the report. Set the status to RESOLVED.");
		}
		User reported = report.getReportedUser();
		switch (action) {
			case CONTENT_REMOVED -> moderationService.remove(report.getTargetType(), report.getTargetId());
			case USER_SUSPENDED -> {
				if (reported == null) {
					throw new BadRequestException("This report has no user to suspend.");
				}
				userModerationService.suspend(adminId, reported.getId(),
						request.adminNote() == null || request.adminNote().isBlank()
								? "Violation of community guidelines (" + report.getReason().name().toLowerCase().replace('_', ' ') + ")"
								: request.adminNote().trim());
			}
			case WARNING -> {
				if (reported == null) {
					throw new BadRequestException("This report has no user to warn.");
				}
				notificationService.notify(reported.getId(), NotificationType.SYSTEM, "A note from the SkillSwap team",
						"We received a report about your activity (" + report.getReason().name().toLowerCase().replace('_', ' ')
								+ "). Please review the community guidelines. Repeated reports may lead to suspension.",
						"/profile", null);
			}
			case NONE -> {
			}
		}
		report.setStatus(request.status());
		if (action != ReportAction.NONE) {
			report.setActionTaken(action);
		}
		report.setAdminNote(request.adminNote() == null || request.adminNote().isBlank() ? report.getAdminNote()
				: request.adminNote().trim());
		if (closing) {
			report.setResolvedBy(userRepository.getReferenceById(adminId));
			report.setResolvedAt(LocalDateTime.now());
		}
		return toDto(reportRepository.save(report));
	}

	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
	}

	static ReportDto toDto(Report r) {
		User reported = r.getReportedUser();
		return new ReportDto(r.getId(), UserSummaryDto.from(r.getReporter()), UserSummaryDto.from(reported),
				reported == null ? null : reported.getEmail(), r.getTargetType(), r.getTargetId(), r.getTargetPreview(),
				r.getReason(), r.getDetails(), r.getStatus(), r.getActionTaken(), r.getAdminNote(),
				r.getResolvedBy() == null ? null : r.getResolvedBy().getFullName(), r.getResolvedAt(), r.getCreatedAt());
	}
}
