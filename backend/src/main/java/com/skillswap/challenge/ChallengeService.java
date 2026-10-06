package com.skillswap.challenge;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.challenge.dto.ChallengeDtos.ChallengeDto;
import com.skillswap.challenge.dto.ChallengeDtos.ReviewDto;
import com.skillswap.challenge.dto.ChallengeDtos.ReviewRequest;
import com.skillswap.challenge.dto.ChallengeDtos.SubmissionDto;
import com.skillswap.challenge.dto.ChallengeDtos.SubmissionRequest;
import com.skillswap.challenge.dto.ChallengeDtos.UpsertChallengeRequest;
import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.reputation.ReputationService;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.security.UserPrincipal;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillService;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

@Service
public class ChallengeService {

	private static final Map<Difficulty, Integer> XP_BY_DIFFICULTY = Map.of(Difficulty.EASY, 50, Difficulty.MEDIUM, 100,
			Difficulty.HARD, 150);

	private final ChallengeRepository challengeRepository;
	private final SubmissionRepository submissionRepository;
	private final SubmissionReviewRepository reviewRepository;
	private final UserRepository userRepository;
	private final SkillService skillService;
	private final ReputationService reputationService;
	private final NotificationService notificationService;

	public ChallengeService(ChallengeRepository challengeRepository, SubmissionRepository submissionRepository,
			SubmissionReviewRepository reviewRepository, UserRepository userRepository, SkillService skillService,
			ReputationService reputationService, NotificationService notificationService) {
		this.challengeRepository = challengeRepository;
		this.submissionRepository = submissionRepository;
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
		this.skillService = skillService;
		this.reputationService = reputationService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public PageResponse<ChallengeDto> search(Long meId, boolean active, Long skillId, Difficulty difficulty, String q,
			int page, int size) {
		Sort sort = active ? Sort.by("deadline").ascending() : Sort.by("deadline").descending();
		Page<Challenge> result = challengeRepository.search(active, LocalDateTime.now(), skillId, difficulty,
				q == null || q.isBlank() ? null : q.trim(), PageRequests.of(page, size, sort));
		List<Long> ids = result.getContent().stream().map(Challenge::getId).toList();
		Map<Long, Long> counts = new HashMap<>();
		Set<Long> submitted = new HashSet<>();
		if (!ids.isEmpty()) {
			submissionRepository.countByChallenges(ids).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
			submitted.addAll(submissionRepository.findSubmittedChallengeIds(meId, ids));
		}
		return PageResponse.from(result,
				c -> toDto(c, meId, counts.getOrDefault(c.getId(), 0L), submitted.contains(c.getId())));
	}

	@Transactional(readOnly = true)
	public ChallengeDto get(Long meId, Long id) {
		Challenge challenge = find(id);
		return toDto(challenge, meId, submissionRepository.countByChallengeId(id),
				submissionRepository.existsByChallengeIdAndUserId(id, meId));
	}

	@Transactional
	public ChallengeDto create(Long meId, UpsertChallengeRequest request) {
		Challenge challenge = new Challenge();
		challenge.setCreator(userRepository.getReferenceById(meId));
		apply(challenge, request);
		challengeRepository.save(challenge);
		return toDto(challenge, meId, 0, false);
	}

	@Transactional
	public ChallengeDto update(UserPrincipal me, Long id, UpsertChallengeRequest request) {
		Challenge challenge = find(id);
		requireCreatorOrAdmin(me, challenge);
		apply(challenge, request);
		return get(me.getId(), id);
	}

	@Transactional
	public void delete(UserPrincipal me, Long id) {
		Challenge challenge = find(id);
		requireCreatorOrAdmin(me, challenge);
		deleteCascade(challenge);
	}

	@Transactional
	public void deleteCascade(Challenge challenge) {
		reviewRepository.deleteByChallenge(challenge.getId());
		submissionRepository.deleteByChallenge(challenge.getId());
		challengeRepository.delete(challenge);
	}

	@Transactional(readOnly = true)
	public List<SubmissionDto> submissions(Long meId, Long challengeId) {
		find(challengeId);
		List<Submission> submissions = submissionRepository.findForChallenge(challengeId);
		return toDtos(meId, submissions);
	}

	@Transactional
	public SubmissionDto submit(Long meId, Long challengeId, SubmissionRequest request) {
		Challenge challenge = find(challengeId);
		if (!challenge.isOpen()) {
			throw new ConflictException("The deadline for this challenge has passed.");
		}
		if (submissionRepository.existsByChallengeIdAndUserId(challengeId, meId)) {
			throw new ConflictException("You have already submitted. Edit your submission instead.");
		}
		requireSomeContent(request);
		Submission submission = new Submission();
		submission.setChallenge(challenge);
		submission.setUser(userRepository.getReferenceById(meId));
		apply(submission, request);
		submissionRepository.save(submission);
		reputationService.addXp(meId, challenge.getXpReward());
		reputationService.evaluateBadges(meId);
		if (!challenge.getCreator().getId().equals(meId)) {
			notificationService.notify(challenge.getCreator().getId(), NotificationType.CHALLENGE_REVIEW,
					"New submission", "Someone submitted a solution to \"" + challenge.getTitle() + "\".",
					"/challenges/" + challengeId, meId);
		}
		return toDtos(meId, List.of(submission)).get(0);
	}

	@Transactional
	public SubmissionDto updateSubmission(Long meId, Long challengeId, SubmissionRequest request) {
		Challenge challenge = find(challengeId);
		if (!challenge.isOpen()) {
			throw new ConflictException("Submissions are locked after the deadline.");
		}
		Submission submission = submissionRepository.findByChallengeIdAndUserId(challengeId, meId)
				.orElseThrow(() -> new ResourceNotFoundException("You have not submitted to this challenge yet."));
		requireSomeContent(request);
		apply(submission, request);
		return toDtos(meId, List.of(submission)).get(0);
	}

	@Transactional
	public ReviewDto review(Long meId, Long submissionId, ReviewRequest request) {
		Submission submission = findSubmission(submissionId);
		if (submission.getUser().getId().equals(meId)) {
			throw new BadRequestException("You cannot review your own submission.");
		}
		if (reviewRepository.existsBySubmissionIdAndReviewerId(submissionId, meId)) {
			throw new ConflictException("You have already reviewed this submission.");
		}
		User me = userRepository.findById(meId).orElseThrow();
		SubmissionReview review = new SubmissionReview();
		review.setSubmission(submission);
		review.setReviewer(me);
		review.setRating(request.rating());
		review.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
		reviewRepository.save(review);
		reputationService.addXp(meId, ReputationService.XP_REVIEW_SUBMISSION);
		notificationService.notify(submission.getUser().getId(), NotificationType.CHALLENGE_REVIEW,
				"Your submission was reviewed",
				me.getFullName() + " rated \"" + submission.getProjectTitle() + "\" " + request.rating() + "/5.",
				"/challenges/" + submission.getChallenge().getId(), meId);
		return toDto(review);
	}

	@Transactional(readOnly = true)
	public List<ReviewDto> reviews(Long submissionId) {
		findSubmission(submissionId);
		return reviewRepository.findForSubmission(submissionId).stream().map(ChallengeService::toDto).toList();
	}

	@Transactional
	public void deleteSubmission(Submission submission) {
		reviewRepository.deleteBySubmission(submission.getId());
		submissionRepository.delete(submission);
	}

	public Challenge find(Long id) {
		return challengeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Challenge", id));
	}

	public Submission findSubmission(Long id) {
		return submissionRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Submission", id));
	}

	private void requireCreatorOrAdmin(UserPrincipal me, Challenge challenge) {
		if (!me.isAdmin() && !challenge.getCreator().getId().equals(me.getId())) {
			throw new ForbiddenException("Only the creator can change this challenge.");
		}
	}

	private static void requireSomeContent(SubmissionRequest request) {
		boolean hasLink = request.githubUrl() != null && !request.githubUrl().isBlank()
				|| request.demoUrl() != null && !request.demoUrl().isBlank();
		boolean hasText = request.submissionText() != null && !request.submissionText().isBlank();
		if (!hasLink && !hasText) {
			throw new BadRequestException("Add a GitHub link, a demo link or a written answer.");
		}
	}

	private void apply(Challenge challenge, UpsertChallengeRequest request) {
		if (!request.deadline().isAfter(LocalDateTime.now().plusHours(1))) {
			throw new BadRequestException("The deadline must be at least an hour from now.");
		}
		if (request.deadline().isAfter(LocalDateTime.now().plusMonths(6))) {
			throw new BadRequestException("The deadline can be at most 6 months away.");
		}
		Skill skill = skillService.requireActive(request.skillId());
		challenge.setTitle(request.title().trim());
		challenge.setDescription(request.description().trim());
		challenge.setSkill(skill);
		challenge.setDifficulty(request.difficulty());
		challenge.setDeadline(request.deadline());
		challenge.setXpReward(XP_BY_DIFFICULTY.get(request.difficulty()));
	}

	private static void apply(Submission submission, SubmissionRequest request) {
		submission.setProjectTitle(request.projectTitle().trim());
		submission.setDescription(request.description().trim());
		submission.setGithubUrl(blankToNull(request.githubUrl()));
		submission.setDemoUrl(blankToNull(request.demoUrl()));
		submission.setSubmissionText(blankToNull(request.submissionText()));
	}

	private List<SubmissionDto> toDtos(Long meId, List<Submission> submissions) {
		List<Long> ids = submissions.stream().map(Submission::getId).toList();
		Map<Long, Object[]> stats = new HashMap<>();
		Set<Long> reviewed = new HashSet<>();
		if (!ids.isEmpty()) {
			reviewRepository.statsForSubmissions(ids).forEach(row -> stats.put((Long) row[0], row));
			reviewed.addAll(reviewRepository.findReviewedSubmissionIds(meId, ids));
		}
		return submissions.stream().map(s -> {
			Object[] row = stats.get(s.getId());
			double avg = row == null ? 0 : UserSummaryDto.round1(((Number) row[1]).doubleValue());
			long count = row == null ? 0 : (Long) row[2];
			return new SubmissionDto(s.getId(), s.getChallenge().getId(), UserSummaryDto.from(s.getUser()),
					s.getProjectTitle(), s.getDescription(), s.getGithubUrl(), s.getDemoUrl(), s.getSubmissionText(),
					avg, count, reviewed.contains(s.getId()), s.getUser().getId().equals(meId), s.getCreatedAt());
		}).toList();
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	static ChallengeDto toDto(Challenge c, Long meId, long submissionCount, boolean submitted) {
		Skill skill = c.getSkill();
		return new ChallengeDto(c.getId(), c.getTitle(), c.getDescription(),
				new SkillRef(skill.getId(), skill.getName(), skill.getCategory().getName(),
						skill.getCategory().getColor()),
				c.getDifficulty(), c.getDeadline(), UserSummaryDto.from(c.getCreator()), c.getXpReward(),
				submissionCount, c.isOpen(), submitted, c.getCreator().getId().equals(meId), c.getCreatedAt());
	}

	static ReviewDto toDto(SubmissionReview r) {
		return new ReviewDto(r.getId(), UserSummaryDto.from(r.getReviewer()), r.getRating(), r.getComment(),
				r.getCreatedAt());
	}
}
