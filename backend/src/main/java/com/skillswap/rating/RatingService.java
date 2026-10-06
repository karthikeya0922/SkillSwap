package com.skillswap.rating;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.rating.dto.RatingDtos.CreateRatingRequest;
import com.skillswap.rating.dto.RatingDtos.RatingSummaryDto;
import com.skillswap.rating.dto.RatingDtos.ReviewDto;
import com.skillswap.reputation.ReputationService;
import com.skillswap.session.LearningSession;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

@Service
public class RatingService {

	private final RatingRepository ratingRepository;
	private final LearningSessionRepository sessionRepository;
	private final UserRepository userRepository;
	private final ReputationService reputationService;
	private final NotificationService notificationService;

	public RatingService(RatingRepository ratingRepository, LearningSessionRepository sessionRepository,
			UserRepository userRepository, ReputationService reputationService,
			NotificationService notificationService) {
		this.ratingRepository = ratingRepository;
		this.sessionRepository = sessionRepository;
		this.userRepository = userRepository;
		this.reputationService = reputationService;
		this.notificationService = notificationService;
	}

	@Transactional
	public ReviewDto rate(Long meId, CreateRatingRequest request) {
		LearningSession session = sessionRepository.findDetailed(request.sessionId())
				.orElseThrow(() -> ResourceNotFoundException.of("Session", request.sessionId()));
		if (!session.isLearner(meId)) {
			throw new ForbiddenException("Only the learner of a session can rate the teacher.");
		}
		if (session.getStatus() != SessionStatus.COMPLETED) {
			throw new ConflictException("You can rate a session once it has been completed.");
		}
		if (ratingRepository.existsBySessionId(session.getId())) {
			throw new ConflictException("You have already rated this session.");
		}
		Rating rating = new Rating();
		rating.setSession(session);
		rating.setRater(session.getLearner());
		rating.setRatee(session.getTeacher());
		rating.setStars(request.stars());
		rating.setTeachingQuality(request.teachingQuality());
		rating.setCommunication(request.communication());
		rating.setKnowledge(request.knowledge());
		rating.setFeedback(request.feedback() == null || request.feedback().isBlank() ? null : request.feedback().trim());
		ratingRepository.save(rating);
		ratingRepository.flush();

		User teacher = session.getTeacher();
		refreshAggregate(teacher);
		if (request.stars() == 5) {
			reputationService.addXp(teacher.getId(), ReputationService.XP_FIVE_STAR);
		}
		else if (request.stars() == 4) {
			reputationService.addXp(teacher.getId(), ReputationService.XP_GOOD_RATING);
		}
		notificationService.notify(teacher.getId(), NotificationType.RATING_RECEIVED, "New " + request.stars() + "★ rating",
				session.getLearner().getFullName() + " rated your " + session.getSkill().getName() + " session "
						+ request.stars() + "/5.",
				"/profile", meId);
		reputationService.evaluateBadges(teacher.getId());
		return toDto(rating);
	}

	/** Recomputes the cached average from the ratings table so it can never drift. */
	@Transactional
	public void refreshAggregate(User user) {
		Object[] stats = ratingRepository.statsForRatee(user.getId()).get(0);
		Double avg = (Double) stats[0];
		Long count = (Long) stats[1];
		user.setRatingAverage(avg == null ? 0 : avg);
		user.setRatingCount(count == null ? 0 : count.intValue());
	}

	@Transactional(readOnly = true)
	public RatingSummaryDto forUser(Long userId, int page, int size) {
		if (!userRepository.existsById(userId)) {
			throw ResourceNotFoundException.of("User", userId);
		}
		Object[] stats = ratingRepository.statsForRatee(userId).get(0);
		Map<Integer, Long> distribution = new LinkedHashMap<>();
		for (int stars = 5; stars >= 1; stars--) {
			distribution.put(stars, 0L);
		}
		List<Object[]> rows = ratingRepository.distributionForRatee(userId);
		rows.forEach(row -> distribution.put((Integer) row[0], (Long) row[1]));
		PageResponse<ReviewDto> reviews = PageResponse.from(ratingRepository.findForRatee(userId,
				PageRequests.of(page, size)), RatingService::toDto);
		return new RatingSummaryDto(round(stats[0]), stats[1] == null ? 0 : (Long) stats[1], round(stats[2]),
				round(stats[3]), round(stats[4]), distribution, reviews);
	}

	private static double round(Object value) {
		return value == null ? 0 : UserSummaryDto.round1(((Number) value).doubleValue());
	}

	static ReviewDto toDto(Rating r) {
		return new ReviewDto(r.getId(), r.getSession().getId(), r.getSession().getSkill().getName(),
				UserSummaryDto.from(r.getRater()), r.getStars(), r.getTeachingQuality(), r.getCommunication(),
				r.getKnowledge(), r.getFeedback(), r.getCreatedAt());
	}
}
