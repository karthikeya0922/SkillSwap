package com.skillswap.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {

	@Query(value = "select p from GroupPost p join fetch p.author where p.group.id = :groupId order by p.createdAt desc, p.id desc",
			countQuery = "select count(p) from GroupPost p where p.group.id = :groupId")
	Page<GroupPost> findForGroup(@Param("groupId") Long groupId, Pageable pageable);

	@Modifying
	@Query("delete from GroupPost p where p.group.id = :groupId")
	void deleteByGroup(@Param("groupId") Long groupId);
}
