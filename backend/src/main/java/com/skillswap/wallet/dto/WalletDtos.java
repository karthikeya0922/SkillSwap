package com.skillswap.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.skillswap.wallet.TransactionType;

public final class WalletDtos {

	private WalletDtos() {
	}

	/**
	 * @param balance  credits in the wallet now (escrowed credits are already deducted)
	 * @param held     credits deducted for accepted sessions that have not completed yet
	 * @param pending  credits requested in sessions the teacher has not answered yet (not deducted)
	 * @param available balance minus pending, i.e. what can still be committed to new requests
	 */
	public record WalletDto(BigDecimal balance, BigDecimal totalEarned, BigDecimal totalSpent, BigDecimal held,
			BigDecimal pending, BigDecimal available) {
	}

	public record TransactionDto(Long id, BigDecimal amount, BigDecimal balanceAfter, TransactionType type,
			String description, Long sessionId, LocalDateTime createdAt) {
	}
}
