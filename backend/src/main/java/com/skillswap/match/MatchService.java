package com.skillswap.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.match.MatchCalculator.MatchProfile;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.match.MatchCalculator.SkillEntry;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** Loads profile data in bulk and runs {@link MatchCalculator} over it. */
@Service
public class MatchService {

	public record ScoredUser(User user, MatchResult result) {
	}

	private final UserRepository userRepository;
	private final UserSkillRepository userSkillRepository;

	public MatchService(UserRepository userRepository, UserSkillRepository userSkillRepository) {
		this.userRepository = userRepository;
		this.userSkillRepository = userSkillRepository;
	}

	/** Every active student who overlaps with the viewer's skills, best match first. */
	@Transactional(readOnly = true)
	public List<ScoredUser> findMatches(Long meId) {
		User me = userRepository.findById(meId).orElse(null);
		if (me == null) {
			return List.of();
		}
		MatchProfile myProfile = profiles(List.of(me)).get(meId);
		Set<Long> candidateIds = new HashSet<>();
		if (!myProfile.learn().isEmpty()) {
			candidateIds.addAll(userSkillRepository.findUserIdsWithSkills(SkillType.TEACH, myProfile.learn().keySet()));
		}
		if (!myProfile.teach().isEmpty()) {
			candidateIds.addAll(userSkillRepository.findUserIdsWithSkills(SkillType.LEARN, myProfile.teach().keySet()));
		}
		candidateIds.remove(meId);
		if (candidateIds.isEmpty()) {
			return List.of();
		}
		List<User> candidates = userRepository.findAllById(candidateIds);
		Map<Long, MatchProfile> profiles = profiles(candidates);
		List<ScoredUser> results = new ArrayList<>();
		for (User candidate : candidates) {
			MatchCalculator.calculate(myProfile, profiles.get(candidate.getId()))
					.ifPresent(result -> results.add(new ScoredUser(candidate, result)));
		}
		results.sort(Comparator.comparingInt((ScoredUser s) -> s.result().score()).reversed()
				.thenComparing(s -> s.user().getRatingAverage(), Comparator.reverseOrder())
				.thenComparing(s -> s.user().getId()));
		return results;
	}

	/** Scores the viewer against a given set of users (used by discovery and profile pages). */
	@Transactional(readOnly = true)
	public Map<Long, MatchResult> scoreAgainst(Long meId, Collection<User> others) {
		Map<Long, MatchResult> results = new HashMap<>();
		if (others.isEmpty()) {
			return results;
		}
		User me = userRepository.findById(meId).orElse(null);
		if (me == null) {
			return results;
		}
		List<User> everyone = new ArrayList<>(others);
		everyone.add(me);
		Map<Long, MatchProfile> profiles = profiles(everyone);
		MatchProfile myProfile = profiles.get(meId);
		for (User other : others) {
			if (!other.getId().equals(meId)) {
				MatchCalculator.calculate(myProfile, profiles.get(other.getId()))
						.ifPresent(result -> results.put(other.getId(), result));
			}
		}
		return results;
	}

	private Map<Long, MatchProfile> profiles(Collection<User> users) {
		Map<Long, User> byId = new LinkedHashMap<>();
		users.forEach(u -> byId.put(u.getId(), u));
		Map<Long, Map<Long, SkillEntry>> teach = new HashMap<>();
		Map<Long, Map<Long, SkillEntry>> learn = new HashMap<>();
		for (UserSkill us : userSkillRepository.findAllForUsers(byId.keySet())) {
			Long userId = us.getUser().getId();
			SkillEntry entry = new SkillEntry(us.getSkill().getId(), us.getSkill().getName(),
					us.getSkill().getCategory().getName(), us.getLevel());
			(us.getType() == SkillType.TEACH ? teach : learn).computeIfAbsent(userId, k -> new HashMap<>())
					.put(entry.skillId(), entry);
		}
		Map<Long, MatchProfile> result = new HashMap<>();
		for (User u : byId.values()) {
			result.put(u.getId(), new MatchProfile(u.getId(), u.getFirstName(), teach.getOrDefault(u.getId(), Map.of()),
					learn.getOrDefault(u.getId(), Map.of()),
					u.getAvailability().isEmpty() ? EnumSet.noneOf(com.skillswap.user.Availability.class)
							: EnumSet.copyOf(u.getAvailability()),
					u.getRatingAverage(), u.getRatingCount()));
		}
		return result;
	}
}
