package com.skillswap.user.dto;

import com.skillswap.reputation.Rank;
import com.skillswap.user.User;

/** Compact public view of a user, embedded in sessions, messages, groups and so on. */
public record UserSummaryDto(Long id, String fullName, String avatarUrl, String college, String department,
		Integer yearOfStudy, double ratingAverage, int ratingCount, String rank, boolean active) {

	public static UserSummaryDto from(User user) {
		if (user == null) {
			return null;
		}
		return new UserSummaryDto(user.getId(), user.getFullName(), user.getAvatarUrl(), user.getCollege(),
				user.getDepartment(), user.getYearOfStudy(), round1(user.getRatingAverage()), user.getRatingCount(),
				Rank.fromXp(user.getXp()).label(), user.isActive());
	}

	public static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}
}
