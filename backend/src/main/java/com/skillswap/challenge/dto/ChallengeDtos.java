package com.skillswap.challenge.dto;

import java.time.LocalDateTime;

import com.skillswap.challenge.Difficulty;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ChallengeDtos {

	private ChallengeDtos() {
	}

	public record ChallengeDto(Long id, String title, String description, SkillRef skill, Difficulty difficulty,
			LocalDateTime deadline, UserSummaryDto creator, int xpReward, long submissionCount, boolean open,
			boolean submitted, boolean mine, LocalDateTime createdAt) {
	}

	public record SubmissionDto(Long id, Long challengeId, UserSummaryDto user, String projectTitle,
			String description, String githubUrl, String demoUrl, String submissionText, double averageRating,
			long reviewCount, boolean reviewedByMe, boolean mine, LocalDateTime createdAt) {
	}

	public record ReviewDto(Long id, UserSummaryDto reviewer, int rating, String comment, LocalDateTime createdAt) {
	}

	public record UpsertChallengeRequest(
			@NotBlank(message = "Title is required") @Size(max = 120, message = "Title can be at most 120 characters") String title,
			@NotBlank(message = "Describe the challenge") @Size(max = 4000, message = "Description can be at most 4000 characters") String description,
			@NotNull(message = "Choose a skill") Long skillId,
			@NotNull(message = "Choose a difficulty") Difficulty difficulty,
			@NotNull(message = "Choose a deadline") LocalDateTime deadline) {
	}

	public record SubmissionRequest(
			@NotBlank(message = "Project title is required") @Size(max = 120) String projectTitle,
			@NotBlank(message = "Describe your solution") @Size(max = 1500) String description,
			@Size(max = 300) @Pattern(regexp = "^$|^https://(www\\.)?github\\.com/.+", message = "Use a https://github.com/... link") String githubUrl,
			@Size(max = 300) @Pattern(regexp = "^$|^https?://.+", message = "Demo link must start with http:// or https://") String demoUrl,
			@Size(max = 4000) String submissionText) {
	}

	public record ReviewRequest(
			@NotNull(message = "Choose a rating") @Min(value = 1, message = "Rating must be 1-5") @Max(value = 5, message = "Rating must be 1-5") Integer rating,
			@Size(max = 1000, message = "Comment can be at most 1000 characters") String comment) {
	}
}
