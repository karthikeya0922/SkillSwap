package com.skillswap.skill;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

	@Query("""
			select us from UserSkill us join fetch us.skill s join fetch s.category
			where us.user.id = :userId order by us.type, s.name
			""")
	List<UserSkill> findAllForUser(@Param("userId") Long userId);

	@Query("""
			select us from UserSkill us join fetch us.skill s join fetch s.category
			where us.user.id in :userIds
			""")
	List<UserSkill> findAllForUsers(@Param("userIds") Collection<Long> userIds);

	boolean existsByUserIdAndSkillIdAndType(Long userId, Long skillId, SkillType type);

	long countByUserIdAndType(Long userId, SkillType type);

	long countBySkillId(Long skillId);

	@Query("""
			select distinct us.user.id from UserSkill us
			where us.type = :type and us.skill.id in :skillIds
			  and us.user.status = com.skillswap.user.UserStatus.ACTIVE
			  and us.user.role = com.skillswap.user.Role.STUDENT
			""")
	List<Long> findUserIdsWithSkills(@Param("type") SkillType type, @Param("skillIds") Collection<Long> skillIds);

	@Query("select us.skill.id, us.type, count(us) from UserSkill us group by us.skill.id, us.type")
	List<Object[]> countBySkillAndType();

	@Query("""
			select us.skill.name, count(us) from UserSkill us
			group by us.skill.id, us.skill.name order by count(us) desc
			""")
	List<Object[]> findMostPopularSkills(Pageable pageable);
}
