package com.skillswap.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.skillswap.connection.dto.ConnectionDtos.ConnectionStateDto;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.reputation.dto.ReputationDtos.BadgeDto;
import com.skillswap.reputation.dto.ReputationDtos.RankDto;
import com.skillswap.skill.dto.SkillDtos.UserSkillDto;
import com.skillswap.user.Availability;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {

	private ProfileDtos() {
	}

	public record ProfileStatsDto(long sessionsTaught, long sessionsLearned, long completedSessions,
			long totalSessions, BigDecimal creditsEarned, long connections) {
	}

	/**
	 * Full profile. {@code email} is only filled in for the owner and admins; {@code connection} and {@code match}
	 * describe the viewer's relationship to this user.
	 */
	public record ProfileDto(Long id, String fullName, String email, String college, String department,
			Integer yearOfStudy, String bio, String avatarUrl, String role, String status, Set<Availability> availability,
			List<String> availabilityLabels, double ratingAverage, int ratingCount, RankDto rank,
			ProfileStatsDto stats, List<UserSkillDto> teachSkills, List<UserSkillDto> learnSkills,
			List<BadgeDto> badges, boolean profileCompleted, boolean online, LocalDateTime memberSince,
			LocalDateTime lastActiveAt, boolean self, ConnectionStateDto connection, MatchResult match) {
	}

	public record UpdateProfileRequest(
			@NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be 2-100 characters") String fullName,
			@NotBlank(message = "College or university is required") @Size(max = 150) String college,
			@NotBlank(message = "Department is required") @Size(max = 100) String department,
			@NotNull(message = "Year of study is required") @Min(value = 1, message = "Year must be between 1 and 6") @Max(value = 6, message = "Year must be between 1 and 6") Integer yearOfStudy,
			@Size(max = 1000, message = "Bio can be at most 1000 characters") String bio,
			Set<Availability> availability,
			Boolean completeOnboarding) {
	}

	public record DiscoverFilters(String q, Long skillId, Long categoryId, String level, Double minRating,
			String department, Availability availability, String sort) {
	}

	public record DiscoverMetaDto(List<String> departments) {
	}
}
