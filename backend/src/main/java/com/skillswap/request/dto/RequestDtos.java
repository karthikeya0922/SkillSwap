package com.skillswap.request.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.skillswap.request.OfferStatus;
import com.skillswap.request.RequestStatus;
import com.skillswap.session.SessionMode;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RequestDtos {

	private RequestDtos() {
	}

	public record SkillRef(Long id, String name, String categoryName, String categoryColor) {
	}

	public record RequestDto(Long id, String title, String description, SkillRef skill, UserSummaryDto learner,
			ProficiencyLevel desiredLevel, String preferredSchedule, SessionMode mode, BigDecimal durationHours,
			RequestStatus status, UserSummaryDto acceptedTeacher, long offerCount, OfferStatus myOfferStatus,
			boolean mine, boolean canOffer, LocalDateTime createdAt) {
	}

	public record OfferDto(Long id, Long requestId, UserSummaryDto teacher, String message, OfferStatus status,
			LocalDateTime createdAt) {
	}

	public record RequestDetailDto(RequestDto request, List<OfferDto> offers, OfferDto myOffer) {
	}

	public record MyOfferDto(OfferDto offer, RequestDto request) {
	}

	public record UpsertRequest(
			@NotNull(message = "Choose the skill you want to learn") Long skillId,
			@NotBlank(message = "Give your request a short title") @Size(max = 120, message = "Title can be at most 120 characters") String title,
			@NotBlank(message = "Describe what you want to learn") @Size(max = 1500, message = "Description can be at most 1500 characters") String description,
			@NotNull(message = "Choose the level you want to reach") ProficiencyLevel desiredLevel,
			@Size(max = 200, message = "Schedule can be at most 200 characters") String preferredSchedule,
			@NotNull(message = "Choose online or offline") SessionMode mode,
			@NotNull(message = "Choose a duration") @DecimalMin(value = "0.5", message = "Duration must be at least 30 minutes") @DecimalMax(value = "20", message = "Duration can be at most 20 hours") BigDecimal durationHours) {
	}

	public record OfferRequest(
			@NotBlank(message = "Add a short message for the learner") @Size(max = 800, message = "Message can be at most 800 characters") String message) {
	}
}
