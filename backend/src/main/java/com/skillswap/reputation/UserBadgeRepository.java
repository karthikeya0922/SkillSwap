package com.skillswap.reputation;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

	@Query("select ub from UserBadge ub join fetch ub.badge where ub.user.id = :userId order by ub.createdAt desc")
	List<UserBadge> findForUser(@Param("userId") Long userId);

	@Query("select ub from UserBadge ub join fetch ub.badge where ub.user.id = :userId order by ub.createdAt desc")
	List<UserBadge> findRecentForUser(@Param("userId") Long userId, Pageable pageable);

	@Query("select ub.badge.code from UserBadge ub where ub.user.id = :userId")
	List<BadgeCode> findCodesForUser(@Param("userId") Long userId);
}
