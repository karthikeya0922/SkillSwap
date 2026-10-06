package com.skillswap.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.auth.AuthService;
import com.skillswap.auth.dto.RegisterRequest;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.InsufficientCreditsException;
import com.skillswap.rating.RatingService;
import com.skillswap.rating.dto.RatingDtos.CreateRatingRequest;
import com.skillswap.session.dto.SessionDtos.BookSessionRequest;
import com.skillswap.session.dto.SessionDtos.SessionDetailsRequest;
import com.skillswap.session.dto.SessionDtos.SessionDto;
import com.skillswap.skill.Category;
import com.skillswap.skill.CategoryRepository;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillRepository;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkillService;
import com.skillswap.skill.dto.SkillDtos.UserSkillRequest;
import com.skillswap.wallet.CreditTransactionRepository;
import com.skillswap.wallet.TimeWalletRepository;
import com.skillswap.wallet.WalletService;

/** Booking rules and the credit economy, end to end through the services. */
@SpringBootTest
@Transactional
class SessionFlowTest {

	@Autowired
	private AuthService authService;
	@Autowired
	private SessionService sessionService;
	@Autowired
	private UserSkillService userSkillService;
	@Autowired
	private WalletService walletService;
	@Autowired
	private RatingService ratingService;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private SkillRepository skillRepository;
	@Autowired
	private LearningSessionRepository sessionRepository;
	@Autowired
	private TimeWalletRepository walletRepository;
	@Autowired
	private CreditTransactionRepository transactionRepository;

	private Long teacher;
	private Long learner;
	private Long java;
	private Long guitar;

	@BeforeEach
	void setUp() {
		Category category = new Category();
		category.setName("Programming-" + System.nanoTime());
		category.setSlug(category.getName().toLowerCase());
		categoryRepository.save(category);
		java = skill(category, "Java-" + System.nanoTime());
		guitar = skill(category, "Guitar-" + System.nanoTime());
		teacher = register("teacher");
		learner = register("learner");
		userSkillService.add(teacher, new UserSkillRequest(java, SkillType.TEACH, ProficiencyLevel.EXPERT, 3.0, null));
	}

	private Long skill(Category category, String name) {
		Skill skill = new Skill();
		skill.setName(name);
		skill.setSlug(name.toLowerCase());
		skill.setCategory(category);
		return skillRepository.save(skill).getId();
	}

	private Long register(String prefix) {
		return authService.register(new RegisterRequest("Test " + prefix, prefix + System.nanoTime() + "@test.dev",
				"secret123", "CBIT", "CSE", 3)).user().id();
	}

	private BookSessionRequest booking(Long teacherId, Long skillId, int daysAhead, int startHour, int endHour) {
		return new BookSessionRequest(teacherId, skillId, LocalDate.now().plusDays(daysAhead),
				LocalTime.of(startHour, 0), LocalTime.of(endHour, 0), SessionMode.ONLINE, null, null, null, null);
	}

	private void assertLedgerMatchesBalance(Long userId) {
		Long walletId = walletRepository.findByUserId(userId).orElseThrow().getId();
		assertThat(transactionRepository.sumForWallet(walletId))
				.isEqualByComparingTo(walletService.balance(userId));
	}

	@Test
	void fullLifecycleMovesCreditsThroughTheLedger() {
		SessionDto booked = sessionService.book(learner, booking(teacher, java, 2, 10, 12));
		assertThat(booked.status()).isEqualTo(SessionStatus.REQUESTED);
		assertThat(booked.credits()).isEqualByComparingTo("2.00");
		assertThat(walletService.balance(learner)).isEqualByComparingTo("5.00");

		SessionDto accepted = sessionService.accept(teacher, booked.id(),
				new SessionDetailsRequest(null, "https://meet.example.com/abc", null));
		assertThat(accepted.status()).isEqualTo(SessionStatus.SCHEDULED);
		assertThat(walletService.balance(learner)).isEqualByComparingTo("3.00");

		// Simulate the session having taken place.
		LearningSession session = sessionRepository.findById(booked.id()).orElseThrow();
		session.setStartTime(session.getStartTime().minusDays(3));
		session.setEndTime(session.getEndTime().minusDays(3));

		SessionDto completed = sessionService.complete(learner, booked.id());
		assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(walletService.balance(teacher)).isEqualByComparingTo("7.00");
		assertLedgerMatchesBalance(teacher);
		assertLedgerMatchesBalance(learner);

		ratingService.rate(learner, new CreateRatingRequest(booked.id(), 5, 5, 4, 5, "Great"));
		assertThatThrownBy(() -> ratingService.rate(learner, new CreateRatingRequest(booked.id(), 4, 4, 4, 4, null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void cancellingAnAcceptedSessionRefundsTheLearner() {
		SessionDto booked = sessionService.book(learner, booking(teacher, java, 3, 15, 16));
		sessionService.accept(teacher, booked.id(), null);
		assertThat(walletService.balance(learner)).isEqualByComparingTo("4.00");

		SessionDto cancelled = sessionService.cancel(learner, booked.id(), "Something came up");
		assertThat(cancelled.status()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(walletService.balance(learner)).isEqualByComparingTo("5.00");
		assertLedgerMatchesBalance(learner);
	}

	@Test
	void cannotBookYourself() {
		assertThatThrownBy(() -> sessionService.book(teacher, booking(teacher, java, 1, 10, 11)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("yourself");
	}

	@Test
	void cannotBookInThePast() {
		assertThatThrownBy(() -> sessionService.book(learner, booking(teacher, java, -1, 10, 11)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("future");
	}

	@Test
	void cannotBookASkillTheTeacherDoesNotTeach() {
		assertThatThrownBy(() -> sessionService.book(learner, booking(teacher, guitar, 1, 10, 11)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("does not teach");
	}

	@Test
	void overlappingBookingsAreRejected() {
		sessionService.book(learner, booking(teacher, java, 4, 10, 12));
		assertThatThrownBy(() -> sessionService.book(learner, booking(teacher, java, 4, 11, 13)))
				.isInstanceOf(ConflictException.class).hasMessageContaining("overlaps");
	}

	@Test
	void cannotCommitMoreCreditsThanTheBalance() {
		sessionService.book(learner, booking(teacher, java, 5, 9, 13));
		assertThatThrownBy(() -> sessionService.book(learner, booking(teacher, java, 6, 9, 11)))
				.isInstanceOf(InsufficientCreditsException.class);
		assertThat(walletService.availableForNewRequests(learner)).isEqualByComparingTo(BigDecimal.ONE);
	}
}
