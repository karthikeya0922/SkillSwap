package com.skillswap.request;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRequestRepository extends JpaRepository<SkillRequest, Long> {

	@Query(value = """
			select r from SkillRequest r join fetch r.skill s join fetch s.category join fetch r.learner
			where r.learner.id = :userId and r.status in :statuses
			order by r.createdAt desc
			""", countQuery = "select count(r) from SkillRequest r where r.learner.id = :userId and r.status in :statuses")
	Page<SkillRequest> findMine(@Param("userId") Long userId, @Param("statuses") Collection<RequestStatus> statuses,
			Pageable pageable);

	/**
	 * Open requests from other students. When {@code teachableOnly} is true only requests for {@code skillIds}
	 * (the viewer's teach skills) are returned.
	 */
	@Query(value = """
			select r from SkillRequest r join fetch r.skill s join fetch s.category join fetch r.learner l
			where r.learner.id <> :userId
			  and r.status in (com.skillswap.request.RequestStatus.OPEN, com.skillswap.request.RequestStatus.PENDING)
			  and l.status = com.skillswap.user.UserStatus.ACTIVE
			  and (:skillId is null or s.id = :skillId)
			  and (:teachableOnly = false or s.id in :skillIds)
			  and (:q is null or lower(r.title) like lower(concat('%', :q, '%'))
			       or lower(s.name) like lower(concat('%', :q, '%')))
			order by r.createdAt desc
			""", countQuery = """
			select count(r) from SkillRequest r
			where r.learner.id <> :userId
			  and r.status in (com.skillswap.request.RequestStatus.OPEN, com.skillswap.request.RequestStatus.PENDING)
			  and r.learner.status = com.skillswap.user.UserStatus.ACTIVE
			  and (:skillId is null or r.skill.id = :skillId)
			  and (:teachableOnly = false or r.skill.id in :skillIds)
			  and (:q is null or lower(r.title) like lower(concat('%', :q, '%'))
			       or lower(r.skill.name) like lower(concat('%', :q, '%')))
			""")
	Page<SkillRequest> browse(@Param("userId") Long userId, @Param("skillId") Long skillId,
			@Param("teachableOnly") boolean teachableOnly, @Param("skillIds") Collection<Long> skillIds,
			@Param("q") String q, Pageable pageable);

	@Query("""
			select r from SkillRequest r join fetch r.skill s join fetch s.category join fetch r.learner
			where r.learner.id = :userId and r.status in :statuses
			order by r.createdAt desc
			""")
	List<SkillRequest> findActiveForLearner(@Param("userId") Long userId,
			@Param("statuses") Collection<RequestStatus> statuses, Pageable pageable);

	long countByStatusIn(Collection<RequestStatus> statuses);
}
