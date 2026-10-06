package com.skillswap.group;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {

	@Query("""
			select e from GroupEvent e join fetch e.createdBy
			where e.group.id = :groupId and e.endTime > :now order by e.startTime asc
			""")
	List<GroupEvent> findUpcoming(@Param("groupId") Long groupId, @Param("now") LocalDateTime now);

	@Query("""
			select e from GroupEvent e join fetch e.createdBy
			where e.group.id = :groupId and e.endTime <= :now order by e.startTime desc
			""")
	List<GroupEvent> findPast(@Param("groupId") Long groupId, @Param("now") LocalDateTime now, Pageable pageable);

	@Modifying
	@Query("delete from GroupEvent e where e.group.id = :groupId")
	void deleteByGroup(@Param("groupId") Long groupId);
}
