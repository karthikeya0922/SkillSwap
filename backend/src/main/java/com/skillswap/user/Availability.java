package com.skillswap.user;

/** Coarse weekly availability slots used for discovery filters and match scoring. */
public enum Availability {
	WEEKDAY_MORNING("Weekday mornings"),
	WEEKDAY_AFTERNOON("Weekday afternoons"),
	WEEKDAY_EVENING("Weekday evenings"),
	WEEKEND_MORNING("Weekend mornings"),
	WEEKEND_AFTERNOON("Weekend afternoons"),
	WEEKEND_EVENING("Weekend evenings");

	private final String label;

	Availability(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
