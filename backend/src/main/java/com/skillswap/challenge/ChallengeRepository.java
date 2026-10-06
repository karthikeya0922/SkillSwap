package com.skillswap.challenge;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

	@Query(value = """
			select c from Challenge c join fetch c.skill s join fetch s.category join fetch c.creator
			where ((:active = true and c.deadline > :now) or (:active = false and c.deadline <= :now))
			  and (:skillId is null or s.id = :skillId)
			  and (:difficulty is null or c.difficulty = :difficulty)
			  and (:q is null or lower(c.title) like lower(concat('%', :q, '%')))
			""", countQuery = """
			select count(c) from Challenge c
			where ((:active = true and c.deadline > :now) or (:active = false and c.deadline <= :now))
			  and (:skillId is null or c.skill.id = :skillId)
			  and (:difficulty is null or c.difficulty = :difficulty)
			  and (:q is null or lower(c.title) like lower(concat('%', :q, '%')))
			""")
	Page<Challenge> search(@Param("active") boolean active, @Param("now") LocalDateTime now,
			@Param("skillId") Long skillId, @Param("difficulty") Difficulty difficulty, @Param("q") String q,
			Pageable pageable);
}
