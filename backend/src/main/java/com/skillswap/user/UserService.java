package com.skillswap.user;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skillswap.chat.PresenceService;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.common.storage.FileStorageService;
import com.skillswap.connection.ConnectionRepository;
import com.skillswap.connection.ConnectionService;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionState;
import com.skillswap.connection.dto.ConnectionDtos.ConnectionStateDto;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.match.MatchService;
import com.skillswap.reputation.ReputationService;
import com.skillswap.security.UserPrincipal;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.SkillMapper;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.dto.ProfileDtos.DiscoverFilters;
import com.skillswap.user.dto.ProfileDtos.ProfileDto;
import com.skillswap.user.dto.ProfileDtos.ProfileStatsDto;
import com.skillswap.user.dto.ProfileDtos.UpdateProfileRequest;
import com.skillswap.user.dto.UserCardDto;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.wallet.CreditTransactionRepository;
import com.skillswap.wallet.TransactionType;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class UserService {

	private static final int DISCOVER_SCAN_LIMIT = 500;

	private final UserRepository userRepository;
	private final UserSkillRepository userSkillRepository;
	private final LearningSessionRepository sessionRepository;
	private final CreditTransactionRepository transactionRepository;
	private final ConnectionRepository connectionRepository;
	private final ConnectionService connectionService;
	private final MatchService matchService;
	private final ReputationService reputationService;
	private final PresenceService presenceService;
	private final UserCardAssembler cardAssembler;
	private final FileStorageService storage;

	public UserService(UserRepository userRepository, UserSkillRepository userSkillRepository,
			LearningSessionRepository sessionRepository, CreditTransactionRepository transactionRepository,
			ConnectionRepository connectionRepository, ConnectionService connectionService, MatchService matchService,
			ReputationService reputationService, PresenceService presenceService, UserCardAssembler cardAssembler,
			FileStorageService storage) {
		this.userRepository = userRepository;
		this.userSkillRepository = userSkillRepository;
		this.sessionRepository = sessionRepository;
		this.transactionRepository = transactionRepository;
		this.connectionRepository = connectionRepository;
		this.connectionService = connectionService;
		this.matchService = matchService;
		this.reputationService = reputationService;
		this.presenceService = presenceService;
		this.cardAssembler = cardAssembler;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public ProfileDto getProfile(UserPrincipal viewer, Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
		boolean self = viewer.getId().equals(userId);
		if (!self && !viewer.isAdmin() && (!user.isActive() || user.isAdmin())) {
			throw ResourceNotFoundException.of("User", userId);
		}
		List<UserSkill> skills = userSkillRepository.findAllForUser(userId);
		long taught = sessionRepository.countByTeacherIdAndStatus(userId, SessionStatus.COMPLETED);
		long learned = sessionRepository.countByLearnerIdAndStatus(userId, SessionStatus.COMPLETED);
		long total = sessionRepository.countForUser(userId, EnumSet.of(SessionStatus.REQUESTED, SessionStatus.ACCEPTED,
				SessionStatus.SCHEDULED, SessionStatus.ONGOING, SessionStatus.COMPLETED));
		ProfileStatsDto stats = new ProfileStatsDto(taught, learned, taught + learned, total,
				transactionRepository.sumForUserAndType(userId, TransactionType.TEACHING_EARNING),
				connectionRepository.countAccepted(userId));
		ConnectionStateDto connection = self ? new ConnectionStateDto(ConnectionState.SELF, null)
				: connectionService.stateBetween(viewer.getId(), userId);
		MatchResult match = self || viewer.isAdmin() ? null
				: matchService.scoreAgainst(viewer.getId(), List.of(user)).get(userId);
		return new ProfileDto(user.getId(), user.getFullName(), self || viewer.isAdmin() ? user.getEmail() : null,
				user.getCollege(), user.getDepartment(), user.getYearOfStudy(), user.getBio(), user.getAvatarUrl(),
				user.getRole().name(), user.getStatus().name(), user.getAvailability(),
				user.getAvailability().stream().sorted().map(Availability::getLabel).toList(),
				UserSummaryDto.round1(user.getRatingAverage()), user.getRatingCount(),
				ReputationService.rankFor(user.getXp()), stats,
				skills.stream().filter(s -> s.getType() == SkillType.TEACH).map(SkillMapper::toDto).toList(),
				skills.stream().filter(s -> s.getType() == SkillType.LEARN).map(SkillMapper::toDto).toList(),
				reputationService.badgesFor(userId), user.isProfileCompleted(), presenceService.isOnline(userId),
				user.getCreatedAt(), user.getLastActiveAt(), self, connection, match);
	}

	@Transactional
	public ProfileDto updateProfile(UserPrincipal me, UpdateProfileRequest request) {
		User user = get(me.getId());
		user.setFullName(request.fullName().trim());
		user.setCollege(request.college().trim());
		user.setDepartment(request.department().trim());
		user.setYearOfStudy(request.yearOfStudy());
		user.setBio(request.bio() == null || request.bio().isBlank() ? null : request.bio().trim());
		if (request.availability() != null) {
			user.getAvailability().clear();
			user.getAvailability().addAll(request.availability());
		}
		if (Boolean.TRUE.equals(request.completeOnboarding())) {
			user.setProfileCompleted(true);
		}
		return getProfile(me, me.getId());
	}

	@Transactional
	public String updateAvatar(Long userId, MultipartFile file) {
		User user = get(userId);
		String url = storage.storeImage(file, "avatars");
		String previous = user.getAvatarUrl();
		user.setAvatarUrl(url);
		storage.deleteByUrl(previous);
		return url;
	}

	@Transactional
	public void removeAvatar(Long userId) {
		User user = get(userId);
		storage.deleteByUrl(user.getAvatarUrl());
		user.setAvatarUrl(null);
	}

	/**
	 * Filtered student search. Filtering happens in the database; the (bounded) result is then scored against the
	 * viewer so it can be sorted by match percentage, and only the requested page is turned into cards.
	 */
	@Transactional(readOnly = true)
	public PageResponse<UserCardDto> discover(Long meId, DiscoverFilters filters, int page, int size) {
		List<User> users = userRepository.findAll(discoverSpec(meId, filters),
				PageRequest.of(0, DISCOVER_SCAN_LIMIT, Sort.by(Sort.Direction.DESC, "ratingAverage"))).getContent();
		Map<Long, MatchResult> matches = matchService.scoreAgainst(meId, users);
		Map<Long, Long> completed = "sessions".equals(filters.sort())
				? cardAssembler.completedCounts(users.stream().map(User::getId).toList())
				: Map.of();
		List<User> sorted = new ArrayList<>(users);
		Comparator<User> byRating = Comparator.comparingDouble(User::getRatingAverage).reversed();
		Comparator<User> order = switch (filters.sort() == null ? "match" : filters.sort()) {
			case "rating" -> byRating.thenComparing(User::getRatingCount, Comparator.reverseOrder());
			case "newest" -> Comparator.comparing(User::getCreatedAt, Comparator.reverseOrder());
			case "name" -> Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER);
			case "sessions" -> Comparator.comparing((User u) -> completed.getOrDefault(u.getId(), 0L)).reversed();
			default -> Comparator.comparingInt((User u) -> matches.containsKey(u.getId()) ? matches.get(u.getId()).score() : 0)
					.reversed().thenComparing(byRating);
		};
		sorted.sort(order.thenComparing(User::getId));
		PageResponse<User> slice = PageResponse.of(sorted, page, Math.min(48, Math.max(1, size)));
		List<UserCardDto> cards = cardAssembler.build(meId, slice.content(), matches);
		return new PageResponse<>(cards, slice.page(), slice.size(), slice.totalElements(), slice.totalPages(),
				slice.last());
	}

	@Transactional(readOnly = true)
	public List<String> departments() {
		return userRepository.findDistinctDepartments();
	}

	private Specification<User> discoverSpec(Long meId, DiscoverFilters f) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("role"), Role.STUDENT));
			predicates.add(cb.equal(root.get("status"), UserStatus.ACTIVE));
			predicates.add(cb.notEqual(root.get("id"), meId));
			if (f.q() != null && !f.q().isBlank()) {
				String like = "%" + f.q().trim().toLowerCase() + "%";
				Subquery<Long> skillMatch = query.subquery(Long.class);
				Root<UserSkill> us = skillMatch.from(UserSkill.class);
				skillMatch.select(us.get("id")).where(cb.equal(us.get("user"), root),
						cb.like(cb.lower(us.get("skill").get("name")), like));
				predicates.add(cb.or(cb.like(cb.lower(root.get("fullName")), like),
						cb.like(cb.lower(root.get("department")), like),
						cb.like(cb.lower(root.get("college")), like), cb.exists(skillMatch)));
			}
			ProficiencyLevel level = parseLevel(f.level());
			if (f.skillId() != null || f.categoryId() != null || level != null) {
				Subquery<Long> teaches = query.subquery(Long.class);
				Root<UserSkill> us = teaches.from(UserSkill.class);
				List<Predicate> inner = new ArrayList<>();
				inner.add(cb.equal(us.get("user"), root));
				inner.add(cb.equal(us.get("type"), SkillType.TEACH));
				if (f.skillId() != null) {
					inner.add(cb.equal(us.get("skill").get("id"), f.skillId()));
				}
				if (f.categoryId() != null) {
					inner.add(cb.equal(us.get("skill").get("category").get("id"), f.categoryId()));
				}
				if (level != null) {
					List<ProficiencyLevel> atLeast = EnumSet.range(level, ProficiencyLevel.EXPERT).stream().toList();
					inner.add(us.get("level").in(atLeast));
				}
				teaches.select(us.get("id")).where(inner.toArray(new Predicate[0]));
				predicates.add(cb.exists(teaches));
			}
			if (f.minRating() != null && f.minRating() > 0) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("ratingAverage"), f.minRating()));
			}
			if (f.department() != null && !f.department().isBlank()) {
				predicates.add(cb.equal(cb.lower(root.get("department")), f.department().trim().toLowerCase()));
			}
			if (f.availability() != null) {
				predicates.add(cb.isMember(f.availability(), root.<java.util.Set<Availability>>get("availability")));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static ProficiencyLevel parseLevel(String level) {
		if (level == null || level.isBlank()) {
			return null;
		}
		try {
			return ProficiencyLevel.valueOf(level.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private User get(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
	}
}
