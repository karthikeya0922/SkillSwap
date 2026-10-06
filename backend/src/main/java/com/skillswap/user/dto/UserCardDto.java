package com.skillswap.user.dto;

import java.util.List;

import com.skillswap.connection.dto.ConnectionDtos.ConnectionStateDto;
import com.skillswap.skill.ProficiencyLevel;

/** Student card used on Discover, Matches and the dashboard. */
public record UserCardDto(Long id, String fullName, String avatarUrl, String college, String department,
		Integer yearOfStudy, String bio, double ratingAverage, int ratingCount, String rank, long sessionsCompleted,
		List<SkillChip> teaches, List<SkillChip> learns, List<String> availability, boolean online,
		ConnectionStateDto connection, MatchSummary match) {

	public record SkillChip(Long skillId, String name, ProficiencyLevel level, String categoryColor) {
	}

	public record MatchSummary(int score, String label, boolean mutual) {
	}
}
