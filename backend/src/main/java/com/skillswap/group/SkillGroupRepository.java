package com.skillswap.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillGroupRepository extends JpaRepository<SkillGroup, Long> {

	/** Public groups plus private groups the viewer belongs to (or is invited to). */
	@Query(value = """
			select g from SkillGroup g join fetch g.skill s join fetch s.category join fetch g.creator
			where (:q is null or lower(g.name) like lower(concat('%', :q, '%'))
			       or lower(s.name) like lower(concat('%', :q, '%')))
			  and (:skillId is null or s.id = :skillId)
			  and (g.privacy = com.skillswap.group.GroupPrivacy.PUBLIC
			       or exists (select 1 from GroupMember m where m.group = g and m.user.id = :userId))
			  and (:mine = false or exists (select 1 from GroupMember m2 where m2.group = g and m2.user.id = :userId
			       and m2.status = com.skillswap.group.MemberStatus.ACTIVE))
			order by g.createdAt desc
			""", countQuery = """
			select count(g) from SkillGroup g
			where (:q is null or lower(g.name) like lower(concat('%', :q, '%'))
			       or lower(g.skill.name) like lower(concat('%', :q, '%')))
			  and (:skillId is null or g.skill.id = :skillId)
			  and (g.privacy = com.skillswap.group.GroupPrivacy.PUBLIC
			       or exists (select 1 from GroupMember m where m.group = g and m.user.id = :userId))
			  and (:mine = false or exists (select 1 from GroupMember m2 where m2.group = g and m2.user.id = :userId
			       and m2.status = com.skillswap.group.MemberStatus.ACTIVE))
			""")
	Page<SkillGroup> search(@Param("userId") Long userId, @Param("q") String q, @Param("skillId") Long skillId,
			@Param("mine") boolean mine, Pageable pageable);

	long countByCreatorId(Long creatorId);
}
