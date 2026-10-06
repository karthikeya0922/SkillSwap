package com.skillswap.group;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

	long countByGroupIdAndStatus(Long groupId, MemberStatus status);

	@Query("""
			select m from GroupMember m join fetch m.user
			where m.group.id = :groupId and m.status = :status order by m.role, m.createdAt
			""")
	List<GroupMember> findMembers(@Param("groupId") Long groupId, @Param("status") MemberStatus status);

	@Query("""
			select m.user.id from GroupMember m
			where m.group.id = :groupId and m.status = com.skillswap.group.MemberStatus.ACTIVE
			""")
	List<Long> findActiveMemberIds(@Param("groupId") Long groupId);

	@Query("""
			select m.group.id, count(m) from GroupMember m
			where m.group.id in :groupIds and m.status = com.skillswap.group.MemberStatus.ACTIVE
			group by m.group.id
			""")
	List<Object[]> countActiveByGroups(@Param("groupIds") Collection<Long> groupIds);

	@Query("select m from GroupMember m where m.user.id = :userId and m.group.id in :groupIds")
	List<GroupMember> findForUserInGroups(@Param("userId") Long userId, @Param("groupIds") Collection<Long> groupIds);

	@Query("""
			select m from GroupMember m join fetch m.group g join fetch g.skill join fetch m.invitedBy
			where m.user.id = :userId and m.status = com.skillswap.group.MemberStatus.INVITED
			order by m.createdAt desc
			""")
	List<GroupMember> findInvitations(@Param("userId") Long userId);

	@Modifying
	@Query("delete from GroupMember m where m.group.id = :groupId")
	void deleteByGroup(@Param("groupId") Long groupId);
}
