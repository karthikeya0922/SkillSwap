package com.skillswap.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.InsufficientCreditsException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** Unit tests for the wallet rules with every repository mocked, so no database or Spring context is involved. */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	private static final Long USER_ID = 7L;

	@Mock
	private TimeWalletRepository walletRepository;
	@Mock
	private CreditTransactionRepository transactionRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private LearningSessionRepository sessionRepository;

	private WalletService walletService;

	@BeforeEach
	void setUp() {
		walletService = new WalletService(walletRepository, transactionRepository, userRepository, sessionRepository,
				new BigDecimal("5"));
	}

	private TimeWallet walletWithBalance(String balance) {
		TimeWallet wallet = new TimeWallet();
		wallet.setBalance(new BigDecimal(balance));
		return wallet;
	}

	private void returnSavedTransactions() {
		when(transactionRepository.save(any(CreditTransaction.class))).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void debitLocksTheWalletAndWritesANegativeLedgerRow() {
		TimeWallet wallet = walletWithBalance("3.00");
		when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
		returnSavedTransactions();

		CreditTransaction tx = walletService.debit(USER_ID, new BigDecimal("1.50"), TransactionType.SESSION_PAYMENT,
				"Python session", null);

		assertThat(wallet.getBalance()).isEqualByComparingTo("1.50");
		assertThat(tx.getAmount()).isEqualByComparingTo("-1.50");
		assertThat(tx.getBalanceAfter()).isEqualByComparingTo("1.50");
		assertThat(tx.getType()).isEqualTo(TransactionType.SESSION_PAYMENT);
		verify(walletRepository).findByUserIdForUpdate(USER_ID);
		verify(walletRepository, never()).findByUserId(anyLong());
	}

	@Test
	void debitMoreThanTheBalanceIsRejectedWithoutTouchingTheLedger() {
		TimeWallet wallet = walletWithBalance("1.00");
		when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletService.debit(USER_ID, new BigDecimal("2"), TransactionType.SESSION_PAYMENT,
				"Too expensive", null))
				.isInstanceOf(InsufficientCreditsException.class)
				.hasMessageContaining("balance is 1");

		assertThat(wallet.getBalance()).isEqualByComparingTo("1.00");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void creditAddsToTheBalanceAndRecordsTheBalanceAfter() {
		TimeWallet wallet = walletWithBalance("2.00");
		when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
		returnSavedTransactions();

		CreditTransaction tx = walletService.credit(USER_ID, new BigDecimal("1.25"), TransactionType.TEACHING_EARNING,
				"Taught Java", null);

		assertThat(wallet.getBalance()).isEqualByComparingTo("3.25");
		assertThat(tx.getAmount()).isEqualByComparingTo("1.25");
		assertThat(tx.getBalanceAfter()).isEqualByComparingTo("3.25");
	}

	@Test
	void zeroOrNegativeAmountsAreRejectedBeforeAnyLookup() {
		assertThatThrownBy(() -> walletService.credit(USER_ID, BigDecimal.ZERO, TransactionType.ADMIN_ADJUSTMENT, "x",
				null)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> walletService.debit(USER_ID, new BigDecimal("-1"), TransactionType.SESSION_PAYMENT,
				"x", null)).isInstanceOf(BadRequestException.class);
		verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
	}

	@Test
	void missingWalletIsReportedAsNotFound() {
		when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> walletService.credit(USER_ID, BigDecimal.ONE, TransactionType.ADMIN_ADJUSTMENT, "x",
				null)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void openingAWalletGrantsTheSignupBonusOnce() {
		when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
		returnSavedTransactions();

		TimeWallet wallet = walletService.openWallet(USER_ID, null);

		assertThat(wallet.getBalance()).isEqualByComparingTo("5.00");
		ArgumentCaptor<CreditTransaction> saved = ArgumentCaptor.forClass(CreditTransaction.class);
		verify(transactionRepository).save(saved.capture());
		assertThat(saved.getValue().getType()).isEqualTo(TransactionType.SIGNUP_BONUS);
		assertThat(saved.getValue().getAmount()).isEqualByComparingTo("5.00");
	}

	@Test
	void openingAnExistingWalletDoesNotPayTheBonusAgain() {
		TimeWallet existing = walletWithBalance("9.00");
		when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

		assertThat(walletService.openWallet(USER_ID, null)).isSameAs(existing);
		verify(walletRepository, never()).save(any());
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void creditsPromisedToPendingRequestsAreNotAvailableForNewOnes() {
		when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(walletWithBalance("4.00")));
		when(sessionRepository.sumCreditsForLearner(USER_ID, List.of(SessionStatus.REQUESTED)))
				.thenReturn(new BigDecimal("1.50"));

		assertThat(walletService.availableForNewRequests(USER_ID)).isEqualByComparingTo("2.50");
	}

	@Test
	void creditsForMinutesRoundsToTwoDecimals() {
		assertThat(WalletService.creditsForMinutes(60)).isEqualByComparingTo("1.00");
		assertThat(WalletService.creditsForMinutes(90)).isEqualByComparingTo("1.50");
		assertThat(WalletService.creditsForMinutes(20)).isEqualByComparingTo("0.33");
	}
}
