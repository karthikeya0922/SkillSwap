package com.skillswap.challenge;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionReviewRepository extends JpaRepository<SubmissionReview, Long> {

	@Query("select r from SubmissionReview r join fetch r.reviewer where r.submission.id = :submissionId order by r.createdAt desc")
	List<SubmissionReview> findForSubmission(@Param("submissionId") Long submissionId);

	boolean existsBySubmissionIdAndReviewerId(Long submissionId, Long reviewerId);

	@Query("""
			select r.submission.id, avg(r.rating), count(r) from SubmissionReview r
			where r.submission.id in :ids group by r.submission.id
			""")
	List<Object[]> statsForSubmissions(@Param("ids") Collection<Long> ids);

	@Query("select r.submission.id from SubmissionReview r where r.reviewer.id = :userId and r.submission.id in :ids")
	List<Long> findReviewedSubmissionIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

	@Modifying
	@Query("delete from SubmissionReview r where r.submission.id = :submissionId")
	void deleteBySubmission(@Param("submissionId") Long submissionId);

	@Modifying
	@Query("delete from SubmissionReview r where r.submission.challenge.id = :challengeId")
	void deleteByChallenge(@Param("challengeId") Long challengeId);
}
