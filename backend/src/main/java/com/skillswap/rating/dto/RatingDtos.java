package com.skillswap.rating.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.skillswap.common.api.PageResponse;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RatingDtos {

	private RatingDtos() {
	}

	public record CreateRatingRequest(
			@NotNull(message = "Session is required") Long sessionId,
			@NotNull(message = "Choose an overall rating") @Min(value = 1, message = "Rating must be 1-5") @Max(value = 5, message = "Rating must be 1-5") Integer stars,
			@NotNull(message = "Rate the teaching quality") @Min(1) @Max(5) Integer teachingQuality,
			@NotNull(message = "Rate the communication") @Min(1) @Max(5) Integer communication,
			@NotNull(message = "Rate the knowledge") @Min(1) @Max(5) Integer knowledge,
			@Size(max = 1000, message = "Feedback can be at most 1000 characters") String feedback) {
	}

	public record ReviewDto(Long id, Long sessionId, String skillName, UserSummaryDto rater, int stars,
			int teachingQuality, int communication, int knowledge, String feedback, LocalDateTime createdAt) {
	}

	public record RatingSummaryDto(double average, long count, double teachingQuality, double communication,
			double knowledge, Map<Integer, Long> distribution, PageResponse<ReviewDto> reviews) {
	}
}
