package com.skillswap.connection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

	@Query("""
			select c from Connection c
			where (c.requester.id = :a and c.addressee.id = :b) or (c.requester.id = :b and c.addressee.id = :a)
			""")
	Optional<Connection> findBetween(@Param("a") Long a, @Param("b") Long b);

	@Query("""
			select c from Connection c join fetch c.requester join fetch c.addressee
			where (c.requester.id = :userId or c.addressee.id = :userId) and c.status = :status
			order by c.updatedAt desc
			""")
	List<Connection> findForUser(@Param("userId") Long userId, @Param("status") ConnectionStatus status);

	@Query("""
			select c from Connection c join fetch c.requester join fetch c.addressee
			where c.addressee.id = :userId and c.status = com.skillswap.connection.ConnectionStatus.PENDING
			order by c.createdAt desc
			""")
	List<Connection> findIncomingPending(@Param("userId") Long userId);

	@Query("""
			select c from Connection c join fetch c.requester join fetch c.addressee
			where c.requester.id = :userId and c.status = com.skillswap.connection.ConnectionStatus.PENDING
			order by c.createdAt desc
			""")
	List<Connection> findOutgoingPending(@Param("userId") Long userId);

	@Query("""
			select count(c) from Connection c
			where (c.requester.id = :userId or c.addressee.id = :userId)
			  and c.status = com.skillswap.connection.ConnectionStatus.ACCEPTED
			""")
	long countAccepted(@Param("userId") Long userId);

	@Query("""
			select case when c.requester.id = :userId then c.addressee.id else c.requester.id end
			from Connection c
			where (c.requester.id = :userId or c.addressee.id = :userId)
			  and c.status = com.skillswap.connection.ConnectionStatus.ACCEPTED
			""")
	List<Long> findConnectedUserIds(@Param("userId") Long userId);

	@Query("""
			select c from Connection c
			where (c.requester.id = :userId and c.addressee.id in :others)
			   or (c.addressee.id = :userId and c.requester.id in :others)
			""")
	List<Connection> findBetweenUserAndOthers(@Param("userId") Long userId, @Param("others") Collection<Long> others);

	long countByStatus(ConnectionStatus status);
}
