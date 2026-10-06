package com.skillswap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.match.MatchService;
import com.skillswap.rating.RatingRepository;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.skill.SkillRepository;
import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.wallet.CreditTransactionRepository;
import com.skillswap.wallet.TimeWallet;
import com.skillswap.wallet.TimeWalletRepository;

@SpringBootTest(properties = { "app.seed.enabled=true", "app.seed.demo-password=demo-pass-123",
		"app.seed.admin-password=admin-pass-123",
		"spring.datasource.url=jdbc:h2:mem:seedtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1" })
@Transactional
class DataSeederTest {

	@Autowired
	private UserRepository users;
	@Autowired
	private SkillRepository skills;
	@Autowired
	private LearningSessionRepository sessions;
	@Autowired
	private RatingRepository ratings;
	@Autowired
	private TimeWalletRepository wallets;
	@Autowired
	private CreditTransactionRepository transactions;
	@Autowired
	private MatchService matchService;

	@Test
	void seedsAPopulatedCampusWithConsistentLedgers() {
		assertThat(users.countByRole(Role.STUDENT)).isGreaterThanOrEqualTo(20);
		assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
		assertThat(skills.count()).isGreaterThanOrEqualTo(30);
		assertThat(sessions.countByStatus(SessionStatus.COMPLETED)).isGreaterThanOrEqualTo(30);
		assertThat(ratings.count()).isGreaterThanOrEqualTo(25);

		for (TimeWallet wallet : wallets.findAll()) {
			assertThat(transactions.sumForWallet(wallet.getId())).isEqualByComparingTo(wallet.getBalance());
			assertThat(wallet.getBalance().signum()).isGreaterThanOrEqualTo(0);
		}

		User karthikeya = users.findByEmailIgnoreCase("karthikeya@skillswap.dev").orElseThrow();
		assertThat(karthikeya.getRatingCount()).isGreaterThan(0);
		var matches = matchService.findMatches(karthikeya.getId());
		assertThat(matches).isNotEmpty();
		assertThat(matches.get(0).result().score()).isGreaterThanOrEqualTo(70);
	}
}
