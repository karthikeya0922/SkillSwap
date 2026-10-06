package com.skillswap.session;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

	@Query("""
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill sk
			join fetch sk.category left join fetch s.skillRequest
			where s.id = :id
			""")
	Optional<LearningSession> findDetailed(@Param("id") Long id);

	/** True if the user (as teacher or learner) has a session in {@code statuses} overlapping [start, end). */
	@Query("""
			select count(s) > 0 from LearningSession s
			where (s.teacher.id = :userId or s.learner.id = :userId)
			  and s.status in :statuses
			  and s.startTime < :end and s.endTime > :start
			  and (:excludeId is null or s.id <> :excludeId)
			""")
	boolean existsOverlap(@Param("userId") Long userId, @Param("statuses") Collection<SessionStatus> statuses,
			@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

	/** True if the learner already has an unanswered request of their own in this time window. */
	@Query("""
			select count(s) > 0 from LearningSession s
			where s.learner.id = :userId and s.status = com.skillswap.session.SessionStatus.REQUESTED
			  and s.startTime < :end and s.endTime > :start
			""")
	boolean existsPendingRequestOverlap(@Param("userId") Long userId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query(value = """
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill sk
			join fetch sk.category
			where ((:role = 'ALL' and (s.teacher.id = :userId or s.learner.id = :userId))
			    or (:role = 'TEACHING' and s.teacher.id = :userId)
			    or (:role = 'LEARNING' and s.learner.id = :userId))
			  and s.status in :statuses
			""", countQuery = """
			select count(s) from LearningSession s
			where ((:role = 'ALL' and (s.teacher.id = :userId or s.learner.id = :userId))
			    or (:role = 'TEACHING' and s.teacher.id = :userId)
			    or (:role = 'LEARNING' and s.learner.id = :userId))
			  and s.status in :statuses
			""")
	Page<LearningSession> findForUser(@Param("userId") Long userId, @Param("role") String role,
			@Param("statuses") Collection<SessionStatus> statuses, Pageable pageable);

	@Query("""
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill sk
			join fetch sk.category
			where (s.teacher.id = :userId or s.learner.id = :userId)
			  and s.status in :statuses and s.endTime > :now
			order by s.startTime asc
			""")
	List<LearningSession> findUpcomingForUser(@Param("userId") Long userId,
			@Param("statuses") Collection<SessionStatus> statuses, @Param("now") LocalDateTime now, Pageable pageable);

	@Query("""
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill
			where s.status in :statuses and s.startTime <= :now
			""")
	List<LearningSession> findStartedInStatuses(@Param("statuses") Collection<SessionStatus> statuses,
			@Param("now") LocalDateTime now);

	@Query("""
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill
			where s.status = com.skillswap.session.SessionStatus.ONGOING and s.endTime <= :cutoff
			""")
	List<LearningSession> findOngoingEndedBefore(@Param("cutoff") LocalDateTime cutoff);

	@Query("""
			select s from LearningSession s join fetch s.teacher join fetch s.learner join fetch s.skill
			where s.status in (com.skillswap.session.SessionStatus.ACCEPTED, com.skillswap.session.SessionStatus.SCHEDULED)
			  and s.reminderSent = false and s.startTime > :now and s.startTime <= :until
			""")
	List<LearningSession> findNeedingReminder(@Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

	long countByTeacherIdAndStatus(Long teacherId, SessionStatus status);

	long countByLearnerIdAndStatus(Long learnerId, SessionStatus status);

	long countByTeacherIdAndStatusIn(Long teacherId, Collection<SessionStatus> statuses);

	@Query("""
			select count(s) from LearningSession s
			where (s.teacher.id = :userId or s.learner.id = :userId) and s.status in :statuses
			""")
	long countForUser(@Param("userId") Long userId, @Param("statuses") Collection<SessionStatus> statuses);

	@Query("""
			select count(distinct s.skill.id) from LearningSession s
			where s.teacher.id = :userId and s.status = com.skillswap.session.SessionStatus.COMPLETED
			""")
	long countDistinctSkillsTaught(@Param("userId") Long userId);

	@Query("""
			select count(distinct s.skill.id) from LearningSession s
			where s.learner.id = :userId and s.status = com.skillswap.session.SessionStatus.COMPLETED
			""")
	long countDistinctSkillsLearned(@Param("userId") Long userId);

	@Query("""
			select s.teacher.id, count(s) from LearningSession s
			where s.teacher.id in :userIds and s.status = com.skillswap.session.SessionStatus.COMPLETED
			group by s.teacher.id
			""")
	List<Object[]> countCompletedAsTeacher(@Param("userIds") Collection<Long> userIds);

	@Query("""
			select s.learner.id, count(s) from LearningSession s
			where s.learner.id in :userIds and s.status = com.skillswap.session.SessionStatus.COMPLETED
			group by s.learner.id
			""")
	List<Object[]> countCompletedAsLearner(@Param("userIds") Collection<Long> userIds);

	@Query("""
			select coalesce(sum(s.credits), 0) from LearningSession s
			where s.learner.id = :userId and s.status in :statuses
			""")
	java.math.BigDecimal sumCreditsForLearner(@Param("userId") Long userId,
			@Param("statuses") Collection<SessionStatus> statuses);

	// ---- Admin analytics ----

	long countByStatus(SessionStatus status);

	@Query("""
			select year(s.startTime), month(s.startTime), s.status, count(s) from LearningSession s
			where s.startTime >= :from
			group by year(s.startTime), month(s.startTime), s.status
			""")
	List<Object[]> countByMonthAndStatus(@Param("from") LocalDateTime from);

	@Query("""
			select c.name, count(s) from LearningSession s join s.skill sk join sk.category c
			group by c.name order by count(s) desc
			""")
	List<Object[]> countByCategory(Pageable pageable);

	@Query("""
			select sk.name, count(s) from LearningSession s join s.skill sk
			where s.status <> com.skillswap.session.SessionStatus.REJECTED
			group by sk.name order by count(s) desc
			""")
	List<Object[]> countBySkill(Pageable pageable);
}
