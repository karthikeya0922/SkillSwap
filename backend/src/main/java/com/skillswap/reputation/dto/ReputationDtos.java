package com.skillswap.reputation.dto;

import java.time.LocalDateTime;

public final class ReputationDtos {

	private ReputationDtos() {
	}

	public record RankDto(String code, String label, int xp, String nextLabel, Integer nextXp, int progress) {
	}

	public record BadgeDto(String code, String name, String description, String icon, String color,
			LocalDateTime awardedAt) {
	}

	public record LeaderboardEntryDto(int position, Long userId, String fullName, String avatarUrl,
			String department, int xp, String rank, double ratingAverage) {
	}
}
