package com.skillswap.reputation;

/** XP thresholds for reputation ranks. */
public enum Rank {
	BEGINNER("Beginner", 0),
	CONTRIBUTOR("Contributor", 200),
	MENTOR("Mentor", 600),
	EXPERT("Expert", 1500),
	COMMUNITY_MASTER("Community Master", 3000);

	private final String label;
	private final int minXp;

	Rank(String label, int minXp) {
		this.label = label;
		this.minXp = minXp;
	}

	public String label() {
		return label;
	}

	public int minXp() {
		return minXp;
	}

	public static Rank fromXp(int xp) {
		Rank result = BEGINNER;
		for (Rank rank : values()) {
			if (xp >= rank.minXp) {
				result = rank;
			}
		}
		return result;
	}

	public Rank next() {
		return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
	}

	/** Progress (0-100) from this rank towards the next one. */
	public int progress(int xp) {
		Rank next = next();
		if (next == null) {
			return 100;
		}
		return (int) Math.min(100, Math.max(0, Math.round((xp - minXp) * 100.0 / (next.minXp - minXp))));
	}
}
