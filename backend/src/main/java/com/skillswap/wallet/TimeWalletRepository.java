package com.skillswap.wallet;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TimeWalletRepository extends JpaRepository<TimeWallet, Long> {

	Optional<TimeWallet> findByUserId(Long userId);

	/** Row-locks the wallet so concurrent debits cannot both pass the balance check. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from TimeWallet w where w.user.id = :userId")
	Optional<TimeWallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
