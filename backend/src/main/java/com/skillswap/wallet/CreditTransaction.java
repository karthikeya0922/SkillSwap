package com.skillswap.wallet;

import java.math.BigDecimal;

import com.skillswap.common.BaseEntity;
import com.skillswap.session.LearningSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable ledger row. Positive amounts are credits in, negative amounts are credits out. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "credit_transactions", indexes = { @Index(name = "idx_tx_wallet_created", columnList = "wallet_id, created_at"),
		@Index(name = "idx_tx_type", columnList = "type") })
public class CreditTransaction extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "wallet_id", nullable = false)
	private TimeWallet wallet;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
	private BigDecimal balanceAfter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransactionType type;

	@Column(nullable = false, length = 200)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private LearningSession session;
}
