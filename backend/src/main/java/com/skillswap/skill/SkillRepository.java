package com.skillswap.skill;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	Optional<Skill> findByNameIgnoreCase(String name);

	long countByActiveTrue();

	long countByCategoryId(Long categoryId);

	/** Number of rows anywhere in the schema that point at this skill. */
	@Query("""
			select (select count(us) from UserSkill us where us.skill.id = :id)
			     + (select count(ls) from LearningSession ls where ls.skill.id = :id)
			     + (select count(sr) from SkillRequest sr where sr.skill.id = :id)
			     + (select count(g) from SkillGroup g where g.skill.id = :id)
			     + (select count(c) from Challenge c where c.skill.id = :id)
			""")
	long countReferences(@Param("id") Long id);

	@Query("""
			select s from Skill s join fetch s.category c
			where (:includeInactive = true or s.active = true)
			  and (:categoryId is null or c.id = :categoryId)
			  and (:q is null or lower(s.name) like lower(concat('%', :q, '%')))
			order by s.name
			""")
	List<Skill> search(@Param("q") String q, @Param("categoryId") Long categoryId,
			@Param("includeInactive") boolean includeInactive);

	@Query(value = """
			select s from Skill s join fetch s.category c
			where (:categoryId is null or c.id = :categoryId)
			  and (:q is null or lower(s.name) like lower(concat('%', :q, '%')))
			""", countQuery = """
			select count(s) from Skill s
			where (:categoryId is null or s.category.id = :categoryId)
			  and (:q is null or lower(s.name) like lower(concat('%', :q, '%')))
			""")
	Page<Skill> adminSearch(@Param("q") String q, @Param("categoryId") Long categoryId, Pageable pageable);
}
