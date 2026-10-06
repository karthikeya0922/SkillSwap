package com.skillswap.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

	@Query(value = """
			select r from Report r join fetch r.reporter left join fetch r.reportedUser left join fetch r.resolvedBy
			where (:status is null or r.status = :status) order by r.createdAt desc
			""", countQuery = "select count(r) from Report r where (:status is null or r.status = :status)")
	Page<Report> search(@Param("status") ReportStatus status, Pageable pageable);

	long countByStatus(ReportStatus status);

	boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTargetType targetType,
			Long targetId, ReportStatus status);
}
