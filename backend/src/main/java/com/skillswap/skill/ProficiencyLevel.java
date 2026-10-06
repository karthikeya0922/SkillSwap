package com.skillswap.skill;

public enum ProficiencyLevel {
	BEGINNER, INTERMEDIATE, ADVANCED, EXPERT;

	public String label() {
		return name().charAt(0) + name().substring(1).toLowerCase();
	}
}
