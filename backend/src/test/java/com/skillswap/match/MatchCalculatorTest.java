package com.skillswap.match;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.skillswap.match.MatchCalculator.MatchProfile;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.match.MatchCalculator.SkillEntry;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.user.Availability;

class MatchCalculatorTest {

	private static final SkillEntry JAVA_EXPERT = new SkillEntry(1L, "Java", "Programming", ProficiencyLevel.EXPERT);
	private static final SkillEntry JAVA_WANT = new SkillEntry(1L, "Java", "Programming", ProficiencyLevel.INTERMEDIATE);
	private static final SkillEntry UX_EXPERT = new SkillEntry(2L, "UI/UX", "UI/UX", ProficiencyLevel.EXPERT);
	private static final SkillEntry UX_WANT = new SkillEntry(2L, "UI/UX", "UI/UX", ProficiencyLevel.INTERMEDIATE);
	private static final SkillEntry GUITAR = new SkillEntry(3L, "Guitar", "Music", ProficiencyLevel.ADVANCED);

	private static MatchProfile profile(long id, String name, Map<Long, SkillEntry> teach, Map<Long, SkillEntry> learn,
			Set<Availability> slots, double rating, int ratings) {
		return new MatchProfile(id, name, teach, learn, slots, rating, ratings);
	}

	@Test
	void twoWaySwapIsAGreatMatchWithAPlainEnglishExplanation() {
		MatchProfile me = profile(1, "Karthikeya", Map.of(1L, JAVA_EXPERT), Map.of(2L, UX_WANT),
				EnumSet.of(Availability.WEEKDAY_EVENING), 0, 0);
		MatchProfile rahul = profile(2, "Rahul", Map.of(2L, UX_EXPERT), Map.of(1L, JAVA_WANT),
				EnumSet.of(Availability.WEEKDAY_EVENING), 4.8, 12);

		MatchResult result = MatchCalculator.calculate(me, rahul).orElseThrow();

		assertThat(result.mutual()).isTrue();
		assertThat(result.score()).isGreaterThanOrEqualTo(80);
		assertThat(result.label()).isEqualTo("Great SkillSwap Match");
		assertThat(result.explanation()).isEqualTo(
				"You want to learn UI/UX and Rahul teaches UI/UX. Rahul wants to learn Java and you teach Java.");
		assertThat(result.sharedAvailability()).containsExactly("weekday evenings");
	}

	@Test
	void oneWayMatchScoresLowerThanTwoWay() {
		MatchProfile me = profile(1, "Me", Map.of(1L, JAVA_EXPERT), Map.of(2L, UX_WANT), Set.of(), 0, 0);
		MatchProfile oneWay = profile(2, "Tanvi", Map.of(2L, UX_EXPERT), Map.of(), Set.of(), 4.8, 12);
		MatchProfile twoWay = profile(3, "Rahul", Map.of(2L, UX_EXPERT), Map.of(1L, JAVA_WANT), Set.of(), 4.8, 12);

		int oneWayScore = MatchCalculator.calculate(me, oneWay).orElseThrow().score();
		int twoWayScore = MatchCalculator.calculate(me, twoWay).orElseThrow().score();

		assertThat(oneWayScore).isLessThan(twoWayScore);
		assertThat(MatchCalculator.calculate(me, oneWay).orElseThrow().mutual()).isFalse();
	}

	@Test
	void noSkillOverlapIsNotAMatch() {
		MatchProfile me = profile(1, "Me", Map.of(1L, JAVA_EXPERT), Map.of(2L, UX_WANT), Set.of(), 0, 0);
		MatchProfile other = profile(2, "Arjun", Map.of(3L, GUITAR), Map.of(), Set.of(), 5, 3);

		assertThat(MatchCalculator.calculate(me, other)).isEmpty();
	}

	@Test
	void teacherBelowDesiredLevelIsPenalised() {
		SkillEntry uxBeginnerTeacher = new SkillEntry(2L, "UI/UX", "UI/UX", ProficiencyLevel.BEGINNER);
		MatchProfile me = profile(1, "Me", Map.of(), Map.of(2L, UX_WANT), Set.of(), 0, 0);
		MatchProfile strong = profile(2, "A", Map.of(2L, UX_EXPERT), Map.of(), Set.of(), 0, 0);
		MatchProfile weak = profile(3, "B", Map.of(2L, uxBeginnerTeacher), Map.of(), Set.of(), 0, 0);

		assertThat(MatchCalculator.calculate(me, weak).orElseThrow().score())
				.isLessThan(MatchCalculator.calculate(me, strong).orElseThrow().score());
	}

	@Test
	void selfIsNeverAMatch() {
		MatchProfile me = profile(1, "Me", Map.of(1L, JAVA_EXPERT), Map.of(1L, JAVA_WANT), Set.of(), 0, 0);
		assertThat(MatchCalculator.calculate(me, me)).isEmpty();
	}
}
