package com.skillswap.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserStatus;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.reputation.Rank;

import jakarta.validation.constraints.Size;

public final class AdminDtos {

	private AdminDtos() {
	}

	public record AdminUserDto(Long id, String fullName, String email, String avatarUrl, String college,
			String department, Integer yearOfStudy, Role role, UserStatus status, String suspensionReason, int xp,
			String rank, double ratingAverage, int ratingCount, LocalDateTime createdAt, LocalDateTime lastActiveAt) {

		public static AdminUserDto from(User u) {
			return new AdminUserDto(u.getId(), u.getFullName(), u.getEmail(), u.getAvatarUrl(), u.getCollege(),
					u.getDepartment(), u.getYearOfStudy(), u.getRole(), u.getStatus(), u.getSuspensionReason(), u.getXp(),
					Rank.fromXp(u.getXp()).label(), UserSummaryDto.round1(u.getRatingAverage()), u.getRatingCount(),
					u.getCreatedAt(), u.getLastActiveAt());
		}
	}

	public record SuspendRequest(@Size(max = 500, message = "Reason can be at most 500 characters") String reason) {
	}

	public record Totals(long totalUsers, long activeUsers, long totalSkills, long totalSessions,
			long completedSessions, long activeRequests, BigDecimal creditsExchanged, double averageRating,
			long openReports, long totalConnections) {
	}

	public record UserGrowthPoint(String month, long newUsers, long totalUsers) {
	}

	public record SessionMonthPoint(String month, long completed, long cancelled, long total) {
	}

	public record NameValue(String name, long value) {
	}

	public record AnalyticsDto(Totals totals, List<UserGrowthPoint> userGrowth, List<SessionMonthPoint> sessionsPerMonth,
			List<NameValue> popularSkills, List<NameValue> activeCategories, List<NameValue> sessionOutcomes) {
	}
}
