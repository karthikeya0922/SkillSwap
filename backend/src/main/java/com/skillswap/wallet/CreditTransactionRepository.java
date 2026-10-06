package com.skillswap.wallet;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

	@Query(value = "select t from CreditTransaction t where t.wallet.user.id = :userId order by t.createdAt desc, t.id desc",
			countQuery = "select count(t) from CreditTransaction t where t.wallet.user.id = :userId")
	Page<CreditTransaction> findForUser(@Param("userId") Long userId, Pageable pageable);

	@Query("select t from CreditTransaction t where t.wallet.user.id = :userId order by t.createdAt desc, t.id desc")
	List<CreditTransaction> findRecentForUser(@Param("userId") Long userId, Pageable pageable);

	@Query("select coalesce(sum(t.amount), 0) from CreditTransaction t where t.wallet.user.id = :userId and t.type = :type")
	BigDecimal sumForUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

	@Query("select coalesce(sum(t.amount), 0) from CreditTransaction t where t.wallet.id = :walletId")
	BigDecimal sumForWallet(@Param("walletId") Long walletId);

	@Query("select coalesce(sum(t.amount), 0) from CreditTransaction t where t.type = :type")
	BigDecimal sumByType(@Param("type") TransactionType type);
}
