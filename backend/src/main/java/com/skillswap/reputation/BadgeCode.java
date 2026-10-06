package com.skillswap.reputation;

/** Badge catalogue. Name, description and look are seeded from here; award rules live in ReputationService. */
public enum BadgeCode {
	FIRST_LESSON("First Lesson", "Completed your first learning session", "BookOpen", "#0ea5e9"),
	FIRST_MENTOR("First Mentor Moment", "Taught your first session", "GraduationCap", "#8b5cf6"),
	DEDICATED_MENTOR("Dedicated Mentor", "Taught 5 completed sessions", "Award", "#f59e0b"),
	MASTER_MENTOR("Master Mentor", "Taught 15 completed sessions", "Crown", "#ef4444"),
	TOP_RATED("Top Rated", "Average rating of 4.5+ from at least 3 reviews", "Star", "#eab308"),
	CHALLENGE_SOLVER("Challenge Solver", "Submitted a solution to a skill challenge", "Trophy", "#10b981"),
	COMMUNITY_BUILDER("Community Builder", "Created a learning group", "Users", "#6366f1"),
	NETWORKER("Networker", "Made 5 connections", "Network", "#14b8a6"),
	HELPING_HAND("Helping Hand", "Had 3 offers of help accepted on learning requests", "HeartHandshake", "#ec4899");

	private final String displayName;
	private final String description;
	private final String icon;
	private final String color;

	BadgeCode(String displayName, String description, String icon, String color) {
		this.displayName = displayName;
		this.description = description;
		this.icon = icon;
		this.color = color;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	public String icon() {
		return icon;
	}

	public String color() {
		return color;
	}
}
