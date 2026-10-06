package com.skillswap.wallet;

import java.math.BigDecimal;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student's credit balance. The balance is only ever changed by {@link WalletService}, which writes a
 * {@link CreditTransaction} for every movement, so the ledger always sums to the balance.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "time_wallets")
public class TimeWallet extends BaseEntity {

	@Setter
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Setter(AccessLevel.PACKAGE)
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@Version
	private Long version;
}
