package com.skillswap.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.chat.PresenceService;
import com.skillswap.connection.ConnectionService;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionStateDto;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.dto.UserCardDto;
import com.skillswap.user.dto.UserCardDto.MatchSummary;
import com.skillswap.user.dto.UserCardDto.SkillChip;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.reputation.Rank;

/** Builds {@link UserCardDto}s for a batch of users with a fixed number of queries. */
@Component
public class UserCardAssembler {

	private final UserSkillRepository userSkillRepository;
	private final LearningSessionRepository sessionRepository;
	private final ConnectionService connectionService;
	private final PresenceService presenceService;

	public UserCardAssembler(UserSkillRepository userSkillRepository, LearningSessionRepository sessionRepository,
			ConnectionService connectionService, PresenceService presenceService) {
		this.userSkillRepository = userSkillRepository;
		this.sessionRepository = sessionRepository;
		this.connectionService = connectionService;
		this.presenceService = presenceService;
	}

	@Transactional(readOnly = true)
	public List<UserCardDto> build(Long viewerId, List<User> users, Map<Long, MatchResult> matches) {
		if (users.isEmpty()) {
			return List.of();
		}
		List<Long> ids = users.stream().map(User::getId).toList();
		Map<Long, List<SkillChip>> teaches = new HashMap<>();
		Map<Long, List<SkillChip>> learns = new HashMap<>();
		for (UserSkill us : userSkillRepository.findAllForUsers(ids)) {
			SkillChip chip = new SkillChip(us.getSkill().getId(), us.getSkill().getName(), us.getLevel(),
					us.getSkill().getCategory().getColor());
			(us.getType() == SkillType.TEACH ? teaches : learns).computeIfAbsent(us.getUser().getId(),
					k -> new ArrayList<>()).add(chip);
		}
		Map<Long, Long> completed = completedCounts(ids);
		Map<Long, ConnectionStateDto> states = connectionService.statesFor(viewerId, ids);
		Set<Long> online = presenceService.onlineAmong(ids);

		List<UserCardDto> cards = new ArrayList<>();
		for (User u : users) {
			MatchResult match = matches == null ? null : matches.get(u.getId());
			cards.add(new UserCardDto(u.getId(), u.getFullName(), u.getAvatarUrl(), u.getCollege(), u.getDepartment(),
					u.getYearOfStudy(), u.getBio(), UserSummaryDto.round1(u.getRatingAverage()), u.getRatingCount(),
					Rank.fromXp(u.getXp()).label(), completed.getOrDefault(u.getId(), 0L),
					teaches.getOrDefault(u.getId(), List.of()), learns.getOrDefault(u.getId(), List.of()),
					u.getAvailability().stream().sorted().map(Availability::getLabel).toList(), online.contains(u.getId()),
					u.getId().equals(viewerId) ? null : states.getOrDefault(u.getId(), ConnectionStateDto.NONE),
					match == null ? null : new MatchSummary(match.score(), match.label(), match.mutual())));
		}
		return cards;
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> completedCounts(Collection<Long> ids) {
		Map<Long, Long> completed = new HashMap<>();
		if (ids.isEmpty()) {
			return completed;
		}
		for (Object[] row : sessionRepository.countCompletedAsTeacher(ids)) {
			completed.merge((Long) row[0], (Long) row[1], Long::sum);
		}
		for (Object[] row : sessionRepository.countCompletedAsLearner(ids)) {
			completed.merge((Long) row[0], (Long) row[1], Long::sum);
		}
		return completed;
	}
}
