package com.skillswap.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.InsufficientCreditsException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.session.LearningSession;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.user.UserRegisteredEvent;
import com.skillswap.user.UserRepository;
import com.skillswap.wallet.dto.WalletDtos.TransactionDto;
import com.skillswap.wallet.dto.WalletDtos.WalletDto;

/**
 * The only component allowed to change a wallet balance. Every change is paired with a ledger row in the same
 * transaction, and debits take a row lock so concurrent spends cannot overdraw the wallet.
 */
@Service
public class WalletService {

	private final TimeWalletRepository walletRepository;
	private final CreditTransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final LearningSessionRepository sessionRepository;
	private final BigDecimal signupBonus;

	public WalletService(TimeWalletRepository walletRepository, CreditTransactionRepository transactionRepository,
			UserRepository userRepository, LearningSessionRepository sessionRepository,
			@Value("${app.wallet.signup-bonus:5}") BigDecimal signupBonus) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.signupBonus = signupBonus;
	}

	@EventListener
	@Transactional
	public void onUserRegistered(UserRegisteredEvent event) {
		openWallet(event.userId(), null);
	}

	/** Creates the user's wallet and grants the starter credits. {@code at} lets seed data backdate the bonus. */
	@Transactional
	public TimeWallet openWallet(Long userId, LocalDateTime at) {
		TimeWallet existing = walletRepository.findByUserId(userId).orElse(null);
		if (existing != null) {
			return existing;
		}
		TimeWallet wallet = new TimeWallet();
		wallet.setUser(userRepository.getReferenceById(userId));
		if (at != null) {
			wallet.setCreatedAt(at);
		}
		walletRepository.save(wallet);
		if (signupBonus.signum() > 0) {
			post(wallet, signupBonus, TransactionType.SIGNUP_BONUS, "Welcome bonus — starter credits", null, at);
		}
		return wallet;
	}

	@Transactional
	public CreditTransaction credit(Long userId, BigDecimal amount, TransactionType type, String description,
			LearningSession session) {
		return credit(userId, amount, type, description, session, null);
	}

	@Transactional
	public CreditTransaction credit(Long userId, BigDecimal amount, TransactionType type, String description,
			LearningSession session, LocalDateTime at) {
		requirePositive(amount);
		TimeWallet wallet = lockedWallet(userId);
		return post(wallet, amount, type, description, session, at);
	}

	@Transactional
	public CreditTransaction debit(Long userId, BigDecimal amount, TransactionType type, String description,
			LearningSession session) {
		return debit(userId, amount, type, description, session, null);
	}

	@Transactional
	public CreditTransaction debit(Long userId, BigDecimal amount, TransactionType type, String description,
			LearningSession session, LocalDateTime at) {
		requirePositive(amount);
		TimeWallet wallet = lockedWallet(userId);
		if (wallet.getBalance().compareTo(amount) < 0) {
			throw new InsufficientCreditsException(amount, wallet.getBalance());
		}
		return post(wallet, amount.negate(), type, description, session, at);
	}

	@Transactional(readOnly = true)
	public BigDecimal balance(Long userId) {
		return wallet(userId).getBalance();
	}

	/** Balance minus credits the user has already committed to session requests still awaiting an answer. */
	@Transactional(readOnly = true)
	public BigDecimal availableForNewRequests(Long userId) {
		BigDecimal pending = sessionRepository.sumCreditsForLearner(userId, List.of(SessionStatus.REQUESTED));
		return balance(userId).subtract(pending);
	}

	@Transactional(readOnly = true)
	public WalletDto summary(Long userId) {
		TimeWallet wallet = wallet(userId);
		BigDecimal earned = transactionRepository.sumForUserAndType(userId, TransactionType.TEACHING_EARNING);
		BigDecimal paid = transactionRepository.sumForUserAndType(userId, TransactionType.SESSION_PAYMENT).negate();
		BigDecimal refunded = transactionRepository.sumForUserAndType(userId, TransactionType.SESSION_REFUND);
		BigDecimal held = sessionRepository.sumCreditsForLearner(userId, LearningSession.CREDITS_HELD);
		BigDecimal pending = sessionRepository.sumCreditsForLearner(userId, List.of(SessionStatus.REQUESTED));
		return new WalletDto(scale(wallet.getBalance()), scale(earned), scale(paid.subtract(refunded)), scale(held),
				scale(pending), scale(wallet.getBalance().subtract(pending).max(BigDecimal.ZERO)));
	}

	@Transactional(readOnly = true)
	public PageResponse<TransactionDto> transactions(Long userId, int page, int size) {
		return PageResponse.from(transactionRepository.findForUser(userId, PageRequests.of(page, size)),
				WalletService::toDto);
	}

	@Transactional(readOnly = true)
	public List<TransactionDto> recentTransactions(Long userId, int limit) {
		return transactionRepository.findRecentForUser(userId, PageRequest.of(0, limit)).stream()
				.map(WalletService::toDto).toList();
	}

	public static BigDecimal creditsForMinutes(int minutes) {
		return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
	}

	private CreditTransaction post(TimeWallet wallet, BigDecimal signedAmount, TransactionType type,
			String description, LearningSession session, LocalDateTime at) {
		BigDecimal newBalance = scale(wallet.getBalance().add(signedAmount));
		wallet.setBalance(newBalance);
		CreditTransaction tx = new CreditTransaction();
		tx.setWallet(wallet);
		tx.setAmount(scale(signedAmount));
		tx.setBalanceAfter(newBalance);
		tx.setType(type);
		tx.setDescription(description);
		tx.setSession(session);
		if (at != null) {
			tx.setCreatedAt(at);
		}
		return transactionRepository.save(tx);
	}

	private TimeWallet wallet(Long userId) {
		return walletRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found for this user"));
	}

	private TimeWallet lockedWallet(Long userId) {
		return walletRepository.findByUserIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found for this user"));
	}

	private static void requirePositive(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new BadRequestException("Credit amounts must be positive.");
		}
	}

	private static BigDecimal scale(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	static TransactionDto toDto(CreditTransaction tx) {
		LearningSession session = tx.getSession();
		return new TransactionDto(tx.getId(), tx.getAmount(), tx.getBalanceAfter(), tx.getType(), tx.getDescription(),
				session == null ? null : session.getId(), tx.getCreatedAt());
	}
}
