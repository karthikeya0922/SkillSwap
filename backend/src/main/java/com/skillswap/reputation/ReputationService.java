package com.skillswap.reputation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.challenge.SubmissionRepository;
import com.skillswap.connection.ConnectionRepository;
import com.skillswap.group.SkillGroupRepository;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.reputation.dto.ReputationDtos.BadgeDto;
import com.skillswap.reputation.dto.ReputationDtos.LeaderboardEntryDto;
import com.skillswap.reputation.dto.ReputationDtos.RankDto;
import com.skillswap.request.OfferStatus;
import com.skillswap.request.RequestOfferRepository;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

/**
 * XP, ranks and badges. Deliberately simple: XP is a running total on the user and badges are re-checked against
 * plain counts after the events that could unlock them.
 */
@Service
public class ReputationService {

	public static final int XP_TEACH_SESSION = 50;
	public static final int XP_LEARN_SESSION = 20;
	public static final int XP_FIVE_STAR = 25;
	public static final int XP_GOOD_RATING = 15;
	public static final int XP_HELPING = 15;
	public static final int XP_REVIEW_SUBMISSION = 5;
	public static final int XP_CREATE_GROUP = 20;

	private final UserRepository userRepository;
	private final BadgeRepository badgeRepository;
	private final UserBadgeRepository userBadgeRepository;
	private final LearningSessionRepository sessionRepository;
	private final SubmissionRepository submissionRepository;
	private final SkillGroupRepository groupRepository;
	private final ConnectionRepository connectionRepository;
	private final RequestOfferRepository offerRepository;
	private final NotificationService notificationService;

	public ReputationService(UserRepository userRepository, BadgeRepository badgeRepository,
			UserBadgeRepository userBadgeRepository, LearningSessionRepository sessionRepository,
			SubmissionRepository submissionRepository, SkillGroupRepository groupRepository,
			ConnectionRepository connectionRepository, RequestOfferRepository offerRepository,
			NotificationService notificationService) {
		this.userRepository = userRepository;
		this.badgeRepository = badgeRepository;
		this.userBadgeRepository = userBadgeRepository;
		this.sessionRepository = sessionRepository;
		this.submissionRepository = submissionRepository;
		this.groupRepository = groupRepository;
		this.connectionRepository = connectionRepository;
		this.offerRepository = offerRepository;
		this.notificationService = notificationService;
	}

	@Transactional
	public void addXp(Long userId, int amount) {
		userRepository.findById(userId).ifPresent(user -> user.setXp(Math.max(0, user.getXp() + amount)));
	}

	/** Creates any badge rows missing from the catalogue. Badges are reference data the code depends on. */
	@Transactional
	public void ensureBadgeCatalog() {
		for (BadgeCode code : BadgeCode.values()) {
			if (badgeRepository.findByCode(code).isEmpty()) {
				Badge badge = new Badge();
				badge.setCode(code);
				badge.setName(code.displayName());
				badge.setDescription(code.description());
				badge.setIcon(code.icon());
				badge.setColor(code.color());
				badgeRepository.save(badge);
			}
		}
	}

	/** Awards every badge the user now qualifies for and does not yet hold. */
	@Transactional
	public List<BadgeCode> evaluateBadges(Long userId, boolean notify) {
		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return List.of();
		}
		Set<BadgeCode> owned = EnumSet.noneOf(BadgeCode.class);
		owned.addAll(userBadgeRepository.findCodesForUser(userId));
		List<BadgeCode> awarded = new ArrayList<>();
		for (BadgeCode code : BadgeCode.values()) {
			if (!owned.contains(code) && qualifies(user, code)) {
				Badge badge = badgeRepository.findByCode(code).orElse(null);
				if (badge == null) {
					continue;
				}
				UserBadge userBadge = new UserBadge();
				userBadge.setUser(user);
				userBadge.setBadge(badge);
				userBadgeRepository.save(userBadge);
				awarded.add(code);
				if (notify) {
					notificationService.notify(userId, NotificationType.BADGE_EARNED, "New badge: " + badge.getName(),
							"You earned the " + badge.getName() + " badge. " + badge.getDescription() + ".",
							"/profile", null);
				}
			}
		}
		return awarded;
	}

	@Transactional
	public List<BadgeCode> evaluateBadges(Long userId) {
		return evaluateBadges(userId, true);
	}

	private boolean qualifies(User user, BadgeCode code) {
		Long id = user.getId();
		return switch (code) {
			case FIRST_LESSON -> sessionRepository.countByLearnerIdAndStatus(id, SessionStatus.COMPLETED) >= 1;
			case FIRST_MENTOR -> sessionRepository.countByTeacherIdAndStatus(id, SessionStatus.COMPLETED) >= 1;
			case DEDICATED_MENTOR -> sessionRepository.countByTeacherIdAndStatus(id, SessionStatus.COMPLETED) >= 5;
			case MASTER_MENTOR -> sessionRepository.countByTeacherIdAndStatus(id, SessionStatus.COMPLETED) >= 15;
			case TOP_RATED -> user.getRatingCount() >= 3 && user.getRatingAverage() >= 4.5;
			case CHALLENGE_SOLVER -> submissionRepository.countByUserId(id) >= 1;
			case COMMUNITY_BUILDER -> groupRepository.countByCreatorId(id) >= 1;
			case NETWORKER -> connectionRepository.countAccepted(id) >= 5;
			case HELPING_HAND -> offerRepository.countByTeacherIdAndStatus(id, OfferStatus.ACCEPTED) >= 3;
		};
	}

	@Transactional(readOnly = true)
	public List<BadgeDto> badgesFor(Long userId) {
		return userBadgeRepository.findForUser(userId).stream().map(ReputationService::toDto).toList();
	}

	@Transactional(readOnly = true)
	public List<BadgeDto> recentBadges(Long userId, int limit) {
		return userBadgeRepository.findRecentForUser(userId, PageRequest.of(0, limit)).stream()
				.map(ReputationService::toDto).toList();
	}

	@Transactional(readOnly = true)
	public List<BadgeDto> catalog() {
		return badgeRepository.findAll().stream()
				.map(b -> new BadgeDto(b.getCode().name(), b.getName(), b.getDescription(), b.getIcon(), b.getColor(),
						null))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaderboardEntryDto> leaderboard(int limit) {
		List<User> users = userRepository.findLeaderboard(PageRequest.of(0, Math.min(50, Math.max(1, limit))));
		List<LeaderboardEntryDto> result = new ArrayList<>();
		for (int i = 0; i < users.size(); i++) {
			User u = users.get(i);
			result.add(new LeaderboardEntryDto(i + 1, u.getId(), u.getFullName(), u.getAvatarUrl(), u.getDepartment(),
					u.getXp(), Rank.fromXp(u.getXp()).label(), UserSummaryDto.round1(u.getRatingAverage())));
		}
		return result;
	}

	public static RankDto rankFor(int xp) {
		Rank rank = Rank.fromXp(xp);
		Rank next = rank.next();
		return new RankDto(rank.name(), rank.label(), xp, next == null ? null : next.label(),
				next == null ? null : next.minXp(), rank.progress(xp));
	}

	private static BadgeDto toDto(UserBadge userBadge) {
		Badge b = userBadge.getBadge();
		return new BadgeDto(b.getCode().name(), b.getName(), b.getDescription(), b.getIcon(), b.getColor(),
				userBadge.getCreatedAt());
	}
}
