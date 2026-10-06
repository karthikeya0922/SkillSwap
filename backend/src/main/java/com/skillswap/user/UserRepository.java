package com.skillswap.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	long countByRole(Role role);

	long countByRoleAndLastActiveAtAfter(Role role, LocalDateTime after);

	long countByRoleAndCreatedAtBefore(Role role, LocalDateTime before);

	@Query("""
			select year(u.createdAt), month(u.createdAt), count(u) from User u
			where u.role = com.skillswap.user.Role.STUDENT and u.createdAt >= :from
			group by year(u.createdAt), month(u.createdAt)
			""")
	List<Object[]> countNewStudentsByMonth(@Param("from") LocalDateTime from);

	@Query("select u from User u where u.role = com.skillswap.user.Role.STUDENT and u.status = com.skillswap.user.UserStatus.ACTIVE order by u.xp desc")
	List<User> findLeaderboard(Pageable pageable);

	@Query("""
			select u from User u
			where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%'))
			       or lower(u.email) like lower(concat('%', :q, '%'))
			       or lower(u.college) like lower(concat('%', :q, '%')))
			  and (:status is null or u.status = :status)
			  and (:role is null or u.role = :role)
			""")
	Page<User> adminSearch(@Param("q") String q, @Param("status") UserStatus status, @Param("role") Role role,
			Pageable pageable);

	@Query("select distinct u.department from User u where u.role = com.skillswap.user.Role.STUDENT order by u.department")
	List<String> findDistinctDepartments();
}
