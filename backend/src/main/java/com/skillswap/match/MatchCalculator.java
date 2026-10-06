package com.skillswap.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.user.Availability;

/**
 * Deterministic, explainable match scoring (no ML). Points out of 100:
 * <ul>
 * <li>up to 50 - the other user teaches skills I want to learn (40 for the first, +5 per extra, minus 3 per skill
 * where their proficiency is below the level I am aiming for)</li>
 * <li>up to 30 - the other user wants skills I teach (25 for the first, +5 for more), making it a two-way swap</li>
 * <li>up to 8 - shared interest categories across both profiles</li>
 * <li>up to 7 - overlapping weekly availability</li>
 * <li>up to 5 - the other user's rating as a teacher (2.5 when unrated)</li>
 * </ul>
 * A pair with no skill overlap in either direction is not a match.
 */
public final class MatchCalculator {

	public record SkillEntry(Long skillId, String skillName, String categoryName, ProficiencyLevel level) {
	}

	public record MatchProfile(Long userId, String firstName, Map<Long, SkillEntry> teach, Map<Long, SkillEntry> learn,
			Set<Availability> availability, double ratingAverage, int ratingCount) {
	}

	public record MatchResult(int score, String label, boolean mutual, List<String> theyCanTeach,
			List<String> youCanTeach, List<String> sharedInterests, List<String> sharedAvailability,
			List<String> reasons, String explanation) {
	}

	private MatchCalculator() {
	}

	public static Optional<MatchResult> calculate(MatchProfile me, MatchProfile other) {
		if (me.userId().equals(other.userId())) {
			return Optional.empty();
		}
		List<SkillEntry> theyTeach = intersect(me.learn(), other.teach());
		List<SkillEntry> theyWant = intersect(other.learn(), me.teach());
		if (theyTeach.isEmpty() && theyWant.isEmpty()) {
			return Optional.empty();
		}

		double score = 0;
		if (!theyTeach.isEmpty()) {
			int levelGaps = 0;
			for (SkillEntry taught : theyTeach) {
				ProficiencyLevel wanted = me.learn().get(taught.skillId()).level();
				if (taught.level().ordinal() < wanted.ordinal()) {
					levelGaps++;
				}
			}
			score += 40 + Math.min(10, (theyTeach.size() - 1) * 5) - Math.min(6, levelGaps * 3);
		}
		if (!theyWant.isEmpty()) {
			score += theyWant.size() > 1 ? 30 : 25;
		}

		Set<String> sharedInterests = new TreeSet<>(categories(me));
		sharedInterests.retainAll(categories(other));
		score += Math.min(8, sharedInterests.size() * 3);

		Set<Availability> sharedSlots = EnumSet.noneOf(Availability.class);
		if (me.availability().isEmpty() || other.availability().isEmpty()) {
			score += 3;
		}
		else {
			sharedSlots.addAll(me.availability());
			sharedSlots.retainAll(other.availability());
			int smaller = Math.min(me.availability().size(), other.availability().size());
			score += 7.0 * sharedSlots.size() / smaller;
		}

		score += other.ratingCount() == 0 ? 2.5 : Math.min(5, other.ratingAverage());

		int finalScore = (int) Math.max(1, Math.min(100, Math.round(score)));
		boolean mutual = !theyTeach.isEmpty() && !theyWant.isEmpty();
		List<String> theyCanTeach = theyTeach.stream().map(SkillEntry::skillName).toList();
		List<String> youCanTeach = theyWant.stream().map(SkillEntry::skillName).toList();
		List<String> slotLabels = sharedSlots.stream().map(a -> a.getLabel().toLowerCase()).toList();

		List<String> reasons = new ArrayList<>();
		String name = other.firstName();
		if (!theyCanTeach.isEmpty()) {
			String skills = humanJoin(theyCanTeach);
			reasons.add("You want to learn " + skills + " and " + name + " teaches " + skills + ".");
		}
		if (!youCanTeach.isEmpty()) {
			String skills = humanJoin(youCanTeach);
			reasons.add(name + " wants to learn " + skills + " and you teach " + skills + ".");
		}
		if (!sharedInterests.isEmpty()) {
			reasons.add("You share interests in " + humanJoin(new ArrayList<>(sharedInterests)) + ".");
		}
		if (!slotLabels.isEmpty()) {
			reasons.add("You are both free on " + humanJoin(slotLabels) + ".");
		}
		if (other.ratingCount() > 0 && other.ratingAverage() >= 4.0) {
			reasons.add(name + " is rated " + String.format("%.1f", other.ratingAverage()) + "★ by "
					+ other.ratingCount() + (other.ratingCount() == 1 ? " learner." : " learners."));
		}
		String explanation = String.join(" ", reasons.subList(0, Math.min(2, reasons.size())));
		return Optional.of(new MatchResult(finalScore, label(finalScore), mutual, theyCanTeach, youCanTeach,
				new ArrayList<>(sharedInterests), slotLabels, reasons, explanation));
	}

	public static String label(int score) {
		if (score >= 80) {
			return "Great SkillSwap Match";
		}
		if (score >= 60) {
			return "Strong Match";
		}
		if (score >= 40) {
			return "Good Match";
		}
		return "Potential Match";
	}

	private static List<SkillEntry> intersect(Map<Long, SkillEntry> wanted, Map<Long, SkillEntry> offered) {
		List<SkillEntry> result = new ArrayList<>();
		for (Long skillId : new TreeSet<>(wanted.keySet())) {
			SkillEntry offer = offered.get(skillId);
			if (offer != null) {
				result.add(offer);
			}
		}
		return result;
	}

	private static Set<String> categories(MatchProfile profile) {
		Set<String> result = new LinkedHashSet<>();
		profile.teach().values().forEach(s -> result.add(s.categoryName()));
		profile.learn().values().forEach(s -> result.add(s.categoryName()));
		return result;
	}

	static String humanJoin(Collection<String> items) {
		List<String> list = new ArrayList<>(items);
		if (list.size() <= 1) {
			return list.isEmpty() ? "" : list.get(0);
		}
		return String.join(", ", list.subList(0, list.size() - 1)) + " and " + list.get(list.size() - 1);
	}
}
