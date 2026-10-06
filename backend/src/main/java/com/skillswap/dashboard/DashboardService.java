package com.skillswap.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.match.MatchQueryService;
import com.skillswap.match.dto.MatchDto;
import com.skillswap.reputation.ReputationService;
import com.skillswap.reputation.dto.ReputationDtos.RankDto;
import com.skillswap.request.SkillRequestService;
import com.skillswap.request.dto.RequestDtos.RequestDto;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionService;
import com.skillswap.session.SessionStatus;
import com.skillswap.session.dto.SessionDtos.SessionDto;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.wallet.WalletService;

@Service
public class DashboardService {

	public record StatsDto(BigDecimal credits, long sessionsCompleted, long sessionsTaught, long sessionsLearned,
			long skillsTaught, long skillsLearned, double rating, int ratingCount) {
	}

	public record ActivityDto(String kind, String title, String detail, BigDecimal amount, String icon, String color,
			LocalDateTime at) {
	}

	public record ProfileCompletionDto(int percent, List<String> missing) {
	}

	public record DashboardDto(String firstName, StatsDto stats, RankDto rank, List<MatchDto> recommendedMatches,
			List<SessionDto> upcomingSessions, long pendingSessionRequests, List<RequestDto> activeRequests,
			List<ActivityDto> recentActivity, ProfileCompletionDto profileCompletion) {
	}

	private final UserRepository userRepository;
	private final UserSkillRepository userSkillRepository;
	private final LearningSessionRepository sessionRepository;
	private final WalletService walletService;
	private final SessionService sessionService;
	private final SkillRequestService requestService;
	private final MatchQueryService matchQueryService;
	private final ReputationService reputationService;

	public DashboardService(UserRepository userRepository, UserSkillRepository userSkillRepository,
			LearningSessionRepository sessionRepository, WalletService walletService, SessionService sessionService,
			SkillRequestService requestService, MatchQueryService matchQueryService,
			ReputationService reputationService) {
		this.userRepository = userRepository;
		this.userSkillRepository = userSkillRepository;
		this.sessionRepository = sessionRepository;
		this.walletService = walletService;
		this.sessionService = sessionService;
		this.requestService = requestService;
		this.matchQueryService = matchQueryService;
		this.reputationService = reputationService;
	}

	@Transactional(readOnly = true)
	public DashboardDto dashboard(Long meId) {
		User me = userRepository.findById(meId).orElseThrow();
		long taught = sessionRepository.countByTeacherIdAndStatus(meId, SessionStatus.COMPLETED);
		long learned = sessionRepository.countByLearnerIdAndStatus(meId, SessionStatus.COMPLETED);
		StatsDto stats = new StatsDto(walletService.balance(meId), taught + learned, taught, learned,
				sessionRepository.countDistinctSkillsTaught(meId), sessionRepository.countDistinctSkillsLearned(meId),
				UserSummaryDto.round1(me.getRatingAverage()), me.getRatingCount());

		List<ActivityDto> activity = new ArrayList<>();
		walletService.recentTransactions(meId, 6).forEach(tx -> activity.add(new ActivityDto("TRANSACTION",
				tx.description(), tx.type().name(), tx.amount(), tx.amount().signum() >= 0 ? "ArrowDownLeft" : "ArrowUpRight",
				tx.amount().signum() >= 0 ? "#10b981" : "#f43f5e", tx.createdAt())));
		reputationService.recentBadges(meId, 4).forEach(b -> activity.add(new ActivityDto("BADGE",
				"Earned the " + b.name() + " badge", b.description(), null, b.icon(), b.color(), b.awardedAt())));
		activity.sort(Comparator.comparing(ActivityDto::at, Comparator.nullsLast(Comparator.reverseOrder())));

		return new DashboardDto(me.getFirstName(), stats, ReputationService.rankFor(me.getXp()),
				matchQueryService.recommended(meId, 4), sessionService.upcoming(meId, 5),
				sessionService.pendingRequestsForTeacher(meId), requestService.activeForLearner(meId, 4),
				activity.subList(0, Math.min(8, activity.size())), completion(me));
	}

	private ProfileCompletionDto completion(User me) {
		List<String> missing = new ArrayList<>();
		if (me.getAvatarUrl() == null) {
			missing.add("Add a profile photo");
		}
		if (me.getBio() == null || me.getBio().isBlank()) {
			missing.add("Write a short bio");
		}
		if (userSkillRepository.countByUserIdAndType(me.getId(), SkillType.TEACH) == 0) {
			missing.add("Add a skill you can teach");
		}
		if (userSkillRepository.countByUserIdAndType(me.getId(), SkillType.LEARN) == 0) {
			missing.add("Add a skill you want to learn");
		}
		if (me.getAvailability().isEmpty()) {
			missing.add("Set your weekly availability");
		}
		int percent = (int) Math.round((5 - missing.size()) * 100.0 / 5);
		return new ProfileCompletionDto(percent, missing);
	}
}
