package com.skillswap.request;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestOfferRepository extends JpaRepository<RequestOffer, Long> {

	@Query("select o from RequestOffer o join fetch o.teacher where o.request.id = :requestId order by o.createdAt")
	List<RequestOffer> findForRequest(@Param("requestId") Long requestId);

	boolean existsByRequestIdAndTeacherId(Long requestId, Long teacherId);

	@Query("select o from RequestOffer o where o.teacher.id = :teacherId and o.request.id in :requestIds")
	List<RequestOffer> findByTeacherAndRequests(@Param("teacherId") Long teacherId,
			@Param("requestIds") Collection<Long> requestIds);

	@Query("""
			select o.request.id, count(o) from RequestOffer o
			where o.request.id in :requestIds and o.status <> com.skillswap.request.OfferStatus.WITHDRAWN
			group by o.request.id
			""")
	List<Object[]> countByRequests(@Param("requestIds") Collection<Long> requestIds);

	@Query(value = """
			select o from RequestOffer o join fetch o.request r join fetch r.skill s join fetch s.category
			join fetch r.learner
			where o.teacher.id = :teacherId order by o.createdAt desc
			""", countQuery = "select count(o) from RequestOffer o where o.teacher.id = :teacherId")
	Page<RequestOffer> findByTeacher(@Param("teacherId") Long teacherId, Pageable pageable);

	long countByTeacherIdAndStatus(Long teacherId, OfferStatus status);

	void deleteByRequestId(Long requestId);
}
