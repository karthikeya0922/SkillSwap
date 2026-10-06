package com.skillswap.challenge;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	@Query("select s from Submission s join fetch s.user where s.challenge.id = :challengeId order by s.createdAt desc")
	List<Submission> findForChallenge(@Param("challengeId") Long challengeId);

	Optional<Submission> findByChallengeIdAndUserId(Long challengeId, Long userId);

	boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

	long countByUserId(Long userId);

	long countByChallengeId(Long challengeId);

	@Query("select s.challenge.id, count(s) from Submission s where s.challenge.id in :ids group by s.challenge.id")
	List<Object[]> countByChallenges(@Param("ids") Collection<Long> ids);

	@Query("select s.challenge.id from Submission s where s.user.id = :userId and s.challenge.id in :ids")
	List<Long> findSubmittedChallengeIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

	@Modifying
	@Query("delete from Submission s where s.challenge.id = :challengeId")
	void deleteByChallenge(@Param("challengeId") Long challengeId);
}
