package com.skillswap.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.session.SessionMode;
import com.skillswap.session.SessionStatus;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SessionDtos {

	private SessionDtos() {
	}

	public record SessionActions(boolean canAccept, boolean canReject, boolean canCancel, boolean canComplete,
			boolean canRate, boolean canJoin, boolean canEditDetails) {
	}

	public record SessionDto(Long id, UserSummaryDto teacher, UserSummaryDto learner, SkillRef skill,
			LocalDate date, @JsonFormat(pattern = "HH:mm") LocalTime startTime,
			@JsonFormat(pattern = "HH:mm") LocalTime endTime, LocalDateTime startAt, LocalDateTime endAt,
			int durationMinutes, BigDecimal credits, SessionMode mode, String location, String meetingLink,
			String notes, SessionStatus status, String myRole, String cancelReason, String cancelledByName,
			LocalDateTime completedAt, boolean rated, Long skillRequestId, LocalDateTime createdAt,
			SessionActions actions) {
	}

	public record BookSessionRequest(
			@NotNull(message = "Choose a teacher") Long teacherId,
			@NotNull(message = "Choose a skill") Long skillId,
			@NotNull(message = "Choose a date") LocalDate date,
			@NotNull(message = "Choose a start time") LocalTime startTime,
			@NotNull(message = "Choose an end time") LocalTime endTime,
			@NotNull(message = "Choose online or offline") SessionMode mode,
			@Size(max = 200, message = "Location can be at most 200 characters") String location,
			@Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Meeting link must start with http:// or https://") String meetingLink,
			@Size(max = 1000, message = "Notes can be at most 1000 characters") String notes,
			Long skillRequestId) {
	}

	public record SessionDetailsRequest(
			@Size(max = 200, message = "Location can be at most 200 characters") String location,
			@Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Meeting link must start with http:// or https://") String meetingLink,
			@Size(max = 1000, message = "Notes can be at most 1000 characters") String notes) {
	}

	public record ReasonRequest(@Size(max = 300, message = "Reason can be at most 300 characters") String reason) {
	}
}
