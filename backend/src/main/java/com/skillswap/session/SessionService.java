package com.skillswap.session;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.InsufficientCreditsException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.rating.RatingRepository;
import com.skillswap.reputation.ReputationService;
import com.skillswap.request.RequestStatus;
import com.skillswap.request.SkillRequest;
import com.skillswap.request.SkillRequestService;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.security.UserPrincipal;
import com.skillswap.session.dto.SessionDtos.BookSessionRequest;
import com.skillswap.session.dto.SessionDtos.SessionActions;
import com.skillswap.session.dto.SessionDtos.SessionDetailsRequest;
import com.skillswap.session.dto.SessionDtos.SessionDto;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillService;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.wallet.TransactionType;
import com.skillswap.wallet.WalletService;

@Service
public class SessionService {

	static final int MIN_MINUTES = 30;
	static final int MAX_MINUTES = 240;
	static final int MAX_DAYS_AHEAD = 90;
	/** How early before the start the "Join" button becomes active. */
	static final Duration JOIN_WINDOW = Duration.ofMinutes(15);
	/** Learners who never confirm do not block the teacher's earnings forever. */
	static final Duration AUTO_COMPLETE_AFTER = Duration.ofHours(48);

	private static final Set<SessionStatus> CANCELLABLE = EnumSet.of(SessionStatus.REQUESTED, SessionStatus.ACCEPTED,
			SessionStatus.SCHEDULED);
	private static final Set<SessionStatus> COMPLETABLE = EnumSet.of(SessionStatus.ACCEPTED, SessionStatus.SCHEDULED,
			SessionStatus.ONGOING);
	private static final String OVERLAP_MESSAGE = "Session cannot be booked because the selected time overlaps with another session.";

	private final LearningSessionRepository sessionRepository;
	private final UserRepository userRepository;
	private final UserSkillRepository userSkillRepository;
	private final SkillService skillService;
	private final WalletService walletService;
	private final NotificationService notificationService;
	private final ReputationService reputationService;
	private final SkillRequestService requestService;
	private final RatingRepository ratingRepository;

	public SessionService(LearningSessionRepository sessionRepository, UserRepository userRepository,
			UserSkillRepository userSkillRepository, SkillService skillService, WalletService walletService,
			NotificationService notificationService, ReputationService reputationService,
			SkillRequestService requestService, RatingRepository ratingRepository) {
		this.sessionRepository = sessionRepository;
		this.userRepository = userRepository;
		this.userSkillRepository = userSkillRepository;
		this.skillService = skillService;
		this.walletService = walletService;
		this.notificationService = notificationService;
		this.reputationService = reputationService;
		this.requestService = requestService;
		this.ratingRepository = ratingRepository;
	}

	@Transactional
	public SessionDto book(Long learnerId, BookSessionRequest body) {
		if (learnerId.equals(body.teacherId())) {
			throw new BadRequestException("You cannot book a session with yourself.");
		}
		User teacher = userRepository.findById(body.teacherId())
				.orElseThrow(() -> ResourceNotFoundException.of("Teacher", body.teacherId()));
		if (!teacher.isActive() || teacher.isAdmin()) {
			throw new BadRequestException("This teacher is not available for sessions.");
		}
		Skill skill = skillService.requireActive(body.skillId());
		if (!userSkillRepository.existsByUserIdAndSkillIdAndType(teacher.getId(), skill.getId(), SkillType.TEACH)) {
			throw new BadRequestException(teacher.getFirstName() + " does not teach " + skill.getName() + ".");
		}

		LocalDateTime start = body.date().atTime(body.startTime());
		LocalDateTime end = body.date().atTime(body.endTime());
		int minutes = validateTimes(start, end);

		BigDecimal credits = WalletService.creditsForMinutes(minutes);
		BigDecimal available = walletService.availableForNewRequests(learnerId);
		if (available.compareTo(credits) < 0) {
			throw new InsufficientCreditsException(credits, available);
		}
		if (sessionRepository.existsOverlap(learnerId, LearningSession.BLOCKING, start, end, null)
				|| sessionRepository.existsPendingRequestOverlap(learnerId, start, end)) {
			throw new ConflictException(OVERLAP_MESSAGE);
		}
		if (sessionRepository.existsOverlap(teacher.getId(), LearningSession.BLOCKING, start, end, null)) {
			throw new ConflictException(teacher.getFirstName() + " already has a session at that time. Try another slot.");
		}

		SkillRequest linked = null;
		if (body.skillRequestId() != null) {
			linked = requestService.find(body.skillRequestId());
			boolean valid = linked.getLearner().getId().equals(learnerId)
					&& linked.getStatus() == RequestStatus.ACCEPTED && linked.getAcceptedTeacher() != null
					&& linked.getAcceptedTeacher().getId().equals(teacher.getId())
					&& linked.getSkill().getId().equals(skill.getId());
			if (!valid) {
				throw new BadRequestException("This session does not match the accepted learning request.");
			}
		}

		User learner = userRepository.getReferenceById(learnerId);
		LearningSession session = new LearningSession();
		session.setTeacher(teacher);
		session.setLearner(learner);
		session.setSkill(skill);
		session.setSkillRequest(linked);
		session.setStartTime(start);
		session.setEndTime(end);
		session.setDurationMinutes(minutes);
		session.setCredits(credits);
		session.setMode(body.mode());
		session.setLocation(blankToNull(body.location()));
		session.setMeetingLink(blankToNull(body.meetingLink()));
		session.setNotes(blankToNull(body.notes()));
		session.setStatus(SessionStatus.REQUESTED);
		sessionRepository.save(session);

		notificationService.notify(teacher.getId(), NotificationType.SESSION_REQUEST, "New session request",
				learner.getFullName() + " wants to learn " + skill.getName() + " on " + Formats.when(start) + ".",
				"/sessions/" + session.getId(), learnerId);
		return toDto(session, learnerId, false);
	}

	@Transactional
	public SessionDto accept(Long meId, Long id, SessionDetailsRequest details) {
		LearningSession session = load(id);
		if (!session.isTeacher(meId)) {
			throw new ForbiddenException("Only the teacher can accept this session.");
		}
		requireStatus(session, EnumSet.of(SessionStatus.REQUESTED), "This session request has already been answered.");
		if (!session.getStartTime().isAfter(LocalDateTime.now())) {
			throw new ConflictException("This request has expired because its start time has passed.");
		}
		if (sessionRepository.existsOverlap(meId, LearningSession.BLOCKING, session.getStartTime(),
				session.getEndTime(), session.getId())) {
			throw new ConflictException("You already have another session at this time.");
		}
		if (sessionRepository.existsOverlap(session.getLearner().getId(), LearningSession.BLOCKING,
				session.getStartTime(), session.getEndTime(), session.getId())) {
			throw new ConflictException("The learner has since booked another session at this time.");
		}
		if (details != null) {
			applyDetails(session, details);
		}
		try {
			walletService.debit(session.getLearner().getId(), session.getCredits(), TransactionType.SESSION_PAYMENT,
					session.getSkill().getName() + " Learning Session with " + session.getTeacher().getFullName(),
					session);
		}
		catch (InsufficientCreditsException ex) {
			throw new InsufficientCreditsException(
					"The learner no longer has enough credits for this session. Ask them to top up by teaching first.");
		}
		session.setStatus(session.hasLogistics() ? SessionStatus.SCHEDULED : SessionStatus.ACCEPTED);
		notificationService.notify(session.getLearner().getId(), NotificationType.SESSION_ACCEPTED,
				"Session accepted",
				session.getTeacher().getFullName() + " accepted your " + session.getSkill().getName() + " session on "
						+ Formats.when(session.getStartTime()) + ". "
						+ Formats.credits(session.getCredits()) + " credits are now held for it.",
				"/sessions/" + id, meId);
		return toDto(session, meId, false);
	}

	@Transactional
	public SessionDto reject(Long meId, Long id, String reason) {
		LearningSession session = load(id);
		if (!session.isTeacher(meId)) {
			throw new ForbiddenException("Only the teacher can decline this session.");
		}
		requireStatus(session, EnumSet.of(SessionStatus.REQUESTED), "This session request has already been answered.");
		session.setStatus(SessionStatus.REJECTED);
		session.setCancelReason(blankToNull(reason));
		session.setCancelledBy(session.getTeacher());
		notificationService.notify(session.getLearner().getId(), NotificationType.SESSION_REJECTED,
				"Session request declined",
				session.getTeacher().getFullName() + " can't take your " + session.getSkill().getName() + " session"
						+ (reason == null || reason.isBlank() ? "." : ": " + reason.trim()),
				"/sessions/" + id, meId);
		return toDto(session, meId, false);
	}

	@Transactional
	public SessionDto cancel(Long meId, Long id, String reason) {
		LearningSession session = load(id);
		if (!session.isParticipant(meId)) {
			throw new ForbiddenException("You are not part of this session.");
		}
		requireStatus(session, CANCELLABLE, "This session can no longer be cancelled.");
		if (!session.getStartTime().isAfter(LocalDateTime.now())) {
			throw new ConflictException("Sessions that have already started cannot be cancelled.");
		}
		User canceller = session.isTeacher(meId) ? session.getTeacher() : session.getLearner();
		cancelInternal(session, canceller, blankToNull(reason));
		return toDto(session, meId, false);
	}

	@Transactional
	public SessionDto updateDetails(Long meId, Long id, SessionDetailsRequest details) {
		LearningSession session = load(id);
		if (!session.isParticipant(meId)) {
			throw new ForbiddenException("You are not part of this session.");
		}
		requireStatus(session, CANCELLABLE, "Details can only be changed before the session starts.");
		applyDetails(session, details);
		if (session.getStatus() == SessionStatus.ACCEPTED && session.hasLogistics()) {
			session.setStatus(SessionStatus.SCHEDULED);
		}
		User other = session.isTeacher(meId) ? session.getLearner() : session.getTeacher();
		User me = session.isTeacher(meId) ? session.getTeacher() : session.getLearner();
		if (session.getStatus() != SessionStatus.REQUESTED) {
			notificationService.notify(other.getId(), NotificationType.SYSTEM, "Session details updated",
					me.getFullName() + " updated the details of your " + session.getSkill().getName() + " session.",
					"/sessions/" + id, meId);
		}
		return toDto(session, meId, false);
	}

	/** The learner confirms the session happened; the held credits are released to the teacher. */
	@Transactional
	public SessionDto complete(Long meId, Long id) {
		LearningSession session = load(id);
		if (!session.isLearner(meId)) {
			throw new ForbiddenException("Only the learner can confirm that the session took place.");
		}
		requireStatus(session, COMPLETABLE, "This session cannot be marked as completed.");
		if (session.getStartTime().isAfter(LocalDateTime.now())) {
			throw new ConflictException("You can mark the session as completed once it has started.");
		}
		completeInternal(session, "confirmed");
		return toDto(session, meId, false);
	}

	@Transactional(readOnly = true)
	public SessionDto get(UserPrincipal me, Long id) {
		LearningSession session = sessionRepository.findDetailed(id)
				.orElseThrow(() -> ResourceNotFoundException.of("Session", id));
		if (!session.isParticipant(me.getId()) && !me.isAdmin()) {
			throw new ForbiddenException("You do not have access to this session.");
		}
		return toDto(session, me.getId(), ratingRepository.existsBySessionId(id));
	}

	/**
	 * @param role  ALL, TEACHING or LEARNING
	 * @param scope upcoming, pending, past or all
	 */
	@Transactional(readOnly = true)
	public PageResponse<SessionDto> list(Long meId, String role, String scope, int page, int size) {
		String normalizedRole = switch (role == null ? "" : role.toUpperCase()) {
			case "TEACHING", "LEARNING" -> role.toUpperCase();
			default -> "ALL";
		};
		Set<SessionStatus> statuses;
		Sort sort;
		switch (scope == null ? "all" : scope.toLowerCase()) {
			case "upcoming" -> {
				statuses = LearningSession.BLOCKING;
				sort = Sort.by("startTime").ascending();
			}
			case "pending" -> {
				statuses = EnumSet.of(SessionStatus.REQUESTED);
				sort = Sort.by("startTime").ascending();
			}
			case "past" -> {
				statuses = EnumSet.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED, SessionStatus.REJECTED);
				sort = Sort.by("startTime").descending();
			}
			default -> {
				statuses = EnumSet.allOf(SessionStatus.class);
				sort = Sort.by("startTime").descending();
			}
		}
		Page<LearningSession> result = sessionRepository.findForUser(meId, normalizedRole, statuses,
				PageRequests.of(page, size, sort));
		List<Long> ids = result.getContent().stream().map(LearningSession::getId).toList();
		Set<Long> rated = ids.isEmpty() ? Set.of() : new HashSet<>(ratingRepository.findRatedSessionIds(ids));
		return PageResponse.from(result, s -> toDto(s, meId, rated.contains(s.getId())));
	}

	@Transactional(readOnly = true)
	public List<SessionDto> upcoming(Long meId, int limit) {
		Set<SessionStatus> statuses = EnumSet.of(SessionStatus.REQUESTED, SessionStatus.ACCEPTED,
				SessionStatus.SCHEDULED, SessionStatus.ONGOING);
		return sessionRepository.findUpcomingForUser(meId, statuses, LocalDateTime.now(), PageRequest.of(0, limit))
				.stream().map(s -> toDto(s, meId, false)).toList();
	}

	@Transactional(readOnly = true)
	public long pendingRequestsForTeacher(Long teacherId) {
		return sessionRepository.countByTeacherIdAndStatusIn(teacherId, EnumSet.of(SessionStatus.REQUESTED));
	}

	/** Cancels a suspended user's future sessions (refunding learners) so counterparts are not left waiting. */
	@Transactional
	public int cancelFutureSessionsOf(Long userId, String reason) {
		List<LearningSession> sessions = sessionRepository.findUpcomingForUser(userId, CANCELLABLE,
				LocalDateTime.now(), PageRequest.of(0, 500));
		int count = 0;
		for (LearningSession session : sessions) {
			if (session.getStartTime().isAfter(LocalDateTime.now())) {
				User actor = session.isTeacher(userId) ? session.getTeacher() : session.getLearner();
				cancelInternal(session, actor, reason);
				count++;
			}
		}
		return count;
	}

	/** Moves sessions through time-based states, sends reminders and expires stale requests. */
	@Transactional
	public void processTimeline() {
		LocalDateTime now = LocalDateTime.now();
		for (LearningSession s : sessionRepository
				.findStartedInStatuses(EnumSet.of(SessionStatus.ACCEPTED, SessionStatus.SCHEDULED), now)) {
			s.setStatus(SessionStatus.ONGOING);
		}
		for (LearningSession s : sessionRepository.findOngoingEndedBefore(now.minus(AUTO_COMPLETE_AFTER))) {
			completeInternal(s, "auto-confirmed " + AUTO_COMPLETE_AFTER.toHours() + " hours after it ended");
			notificationService.notify(s.getLearner().getId(), NotificationType.SESSION_COMPLETED,
					"Session auto-completed", "Your " + s.getSkill().getName() + " session with "
							+ s.getTeacher().getFullName() + " was marked complete automatically. You can still rate it.",
					"/sessions/" + s.getId(), null);
		}
		for (LearningSession s : sessionRepository.findStartedInStatuses(EnumSet.of(SessionStatus.REQUESTED), now)) {
			s.setStatus(SessionStatus.CANCELLED);
			s.setCancelReason("The request expired before the teacher responded.");
			notificationService.notify(s.getLearner().getId(), NotificationType.SESSION_CANCELLED,
					"Session request expired",
					"Your " + s.getSkill().getName() + " request to " + s.getTeacher().getFullName()
							+ " expired without a response. No credits were charged.",
					"/sessions/" + s.getId(), null);
		}
		for (LearningSession s : sessionRepository.findNeedingReminder(now, now.plusMinutes(60))) {
			s.setReminderSent(true);
			String when = "starts at " + Formats.time(s.getStartTime());
			notificationService.notify(s.getTeacher().getId(), NotificationType.UPCOMING_SESSION,
					"Upcoming session", "Your " + s.getSkill().getName() + " session with "
							+ s.getLearner().getFullName() + " " + when + ".",
					"/sessions/" + s.getId(), null);
			notificationService.notify(s.getLearner().getId(), NotificationType.UPCOMING_SESSION,
					"Upcoming session", "Your " + s.getSkill().getName() + " session with "
							+ s.getTeacher().getFullName() + " " + when + ".",
					"/sessions/" + s.getId(), null);
		}
	}

	private void completeInternal(LearningSession session, String how) {
		session.setStatus(SessionStatus.COMPLETED);
		session.setCompletedAt(LocalDateTime.now());
		Long teacherId = session.getTeacher().getId();
		Long learnerId = session.getLearner().getId();
		walletService.credit(teacherId, session.getCredits(), TransactionType.TEACHING_EARNING,
				session.getSkill().getName() + " Teaching Session with " + session.getLearner().getFullName(), session);
		reputationService.addXp(teacherId, ReputationService.XP_TEACH_SESSION);
		reputationService.addXp(learnerId, ReputationService.XP_LEARN_SESSION);
		requestService.markCompletedBySession(session.getSkillRequest());
		notificationService.notify(teacherId, NotificationType.SESSION_COMPLETED, "Session completed",
				"Your " + session.getSkill().getName() + " session with " + session.getLearner().getFullName() + " was "
						+ how + ". +" + Formats.credits(session.getCredits()) + " credits earned!",
				"/wallet", learnerId);
		reputationService.evaluateBadges(teacherId);
		reputationService.evaluateBadges(learnerId);
	}

	private void cancelInternal(LearningSession session, User canceller, String reason) {
		boolean creditsHeld = LearningSession.CREDITS_HELD.contains(session.getStatus());
		session.setStatus(SessionStatus.CANCELLED);
		session.setCancelledBy(canceller);
		session.setCancelReason(reason);
		if (creditsHeld) {
			walletService.credit(session.getLearner().getId(), session.getCredits(), TransactionType.SESSION_REFUND,
					"Refund — cancelled " + session.getSkill().getName() + " session", session);
		}
		User other = session.isTeacher(canceller.getId()) ? session.getLearner() : session.getTeacher();
		notificationService.notify(other.getId(), NotificationType.SESSION_CANCELLED, "Session cancelled",
				canceller.getFullName() + " cancelled the " + session.getSkill().getName() + " session on "
						+ Formats.when(session.getStartTime())
						+ (creditsHeld ? ". Held credits were refunded to the learner." : "."),
				"/sessions/" + session.getId(), canceller.getId());
	}

	private int validateTimes(LocalDateTime start, LocalDateTime end) {
		LocalDateTime now = LocalDateTime.now();
		if (!start.isAfter(now)) {
			throw new BadRequestException("Choose a start time in the future.");
		}
		if (start.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
			throw new BadRequestException("Sessions can be booked up to " + MAX_DAYS_AHEAD + " days ahead.");
		}
		if (!end.isAfter(start)) {
			throw new BadRequestException("The end time must be after the start time.");
		}
		long minutes = Duration.between(start, end).toMinutes();
		if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
			throw new BadRequestException("Sessions must be between 30 minutes and 4 hours long.");
		}
		if (minutes % 15 != 0) {
			throw new BadRequestException("Session length must be in 15-minute steps.");
		}
		return (int) minutes;
	}

	private static void applyDetails(LearningSession session, SessionDetailsRequest details) {
		if (details.location() != null) {
			session.setLocation(blankToNull(details.location()));
		}
		if (details.meetingLink() != null) {
			session.setMeetingLink(blankToNull(details.meetingLink()));
		}
		if (details.notes() != null) {
			session.setNotes(blankToNull(details.notes()));
		}
	}

	private static void requireStatus(LearningSession session, Set<SessionStatus> allowed, String message) {
		if (!allowed.contains(session.getStatus())) {
			throw new ConflictException(message);
		}
	}

	private LearningSession load(Long id) {
		return sessionRepository.findDetailed(id).orElseThrow(() -> ResourceNotFoundException.of("Session", id));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	static SessionDto toDto(LearningSession s, Long viewerId, boolean rated) {
		LocalDateTime now = LocalDateTime.now();
		boolean teacher = s.isTeacher(viewerId);
		boolean learner = s.isLearner(viewerId);
		SessionStatus status = s.getStatus();
		boolean future = s.getStartTime().isAfter(now);
		SessionActions actions = new SessionActions(
				teacher && status == SessionStatus.REQUESTED && future,
				teacher && status == SessionStatus.REQUESTED,
				(teacher || learner) && CANCELLABLE.contains(status) && future,
				learner && COMPLETABLE.contains(status) && !future,
				learner && status == SessionStatus.COMPLETED && !rated,
				(teacher || learner) && LearningSession.BLOCKING.contains(status)
						&& !now.isBefore(s.getStartTime().minus(JOIN_WINDOW)) && now.isBefore(s.getEndTime()),
				(teacher || learner) && CANCELLABLE.contains(status) && future);
		Skill skill = s.getSkill();
		return new SessionDto(s.getId(), UserSummaryDto.from(s.getTeacher()), UserSummaryDto.from(s.getLearner()),
				new SkillRef(skill.getId(), skill.getName(), skill.getCategory().getName(),
						skill.getCategory().getColor()),
				s.getStartTime().toLocalDate(), s.getStartTime().toLocalTime(), s.getEndTime().toLocalTime(),
				s.getStartTime(), s.getEndTime(), s.getDurationMinutes(), s.getCredits(), s.getMode(), s.getLocation(),
				s.getMeetingLink(), s.getNotes(), status, teacher ? "TEACHER" : learner ? "LEARNER" : "VIEWER",
				s.getCancelReason(), s.getCancelledBy() == null ? null : s.getCancelledBy().getFullName(),
				s.getCompletedAt(), rated, s.getSkillRequest() == null ? null : s.getSkillRequest().getId(),
				s.getCreatedAt(), actions);
	}
}
