package com.skillswap.rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	boolean existsBySessionId(Long sessionId);

	Optional<Rating> findBySessionId(Long sessionId);

	@Query("select r.session.id from Rating r where r.session.id in :sessionIds")
	List<Long> findRatedSessionIds(@Param("sessionIds") Collection<Long> sessionIds);

	@Query(value = """
			select r from Rating r join fetch r.rater join fetch r.session s join fetch s.skill
			where r.ratee.id = :userId order by r.createdAt desc
			""", countQuery = "select count(r) from Rating r where r.ratee.id = :userId")
	Page<Rating> findForRatee(@Param("userId") Long userId, Pageable pageable);

	/** avg(stars), count, avg(teaching), avg(communication), avg(knowledge). */
	@Query("""
			select avg(r.stars), count(r), avg(r.teachingQuality), avg(r.communication), avg(r.knowledge)
			from Rating r where r.ratee.id = :userId
			""")
	List<Object[]> statsForRatee(@Param("userId") Long userId);

	@Query("select r.stars, count(r) from Rating r where r.ratee.id = :userId group by r.stars")
	List<Object[]> distributionForRatee(@Param("userId") Long userId);

	@Query("select avg(r.stars) from Rating r")
	Double averageStars();
}
