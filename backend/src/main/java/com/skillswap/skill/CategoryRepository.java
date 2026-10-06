package com.skillswap.skill;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<Category> findAllByOrderByNameAsc();

	@Query("select s.category.id, count(s) from Skill s where s.active = true group by s.category.id")
	List<Object[]> countActiveSkillsByCategory();
}
