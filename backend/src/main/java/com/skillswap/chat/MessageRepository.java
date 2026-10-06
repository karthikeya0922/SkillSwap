package com.skillswap.chat;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

	/** Newest-first page of a conversation, optionally older than {@code beforeId} (for infinite scroll). */
	@Query("""
			select m from Message m
			where ((m.sender.id = :a and m.recipient.id = :b) or (m.sender.id = :b and m.recipient.id = :a))
			  and (:beforeId is null or m.id < :beforeId)
			order by m.id desc
			""")
	List<Message> findConversation(@Param("a") Long a, @Param("b") Long b, @Param("beforeId") Long beforeId,
			Pageable pageable);

	@Query("select max(m.id) from Message m where m.sender.id = :userId group by m.recipient.id")
	List<Long> findLatestSentPerPartner(@Param("userId") Long userId);

	@Query("select max(m.id) from Message m where m.recipient.id = :userId group by m.sender.id")
	List<Long> findLatestReceivedPerPartner(@Param("userId") Long userId);

	@Query("select m from Message m join fetch m.sender join fetch m.recipient where m.id in :ids")
	List<Message> findWithParticipants(@Param("ids") Collection<Long> ids);

	@Query("""
			select m.sender.id, count(m) from Message m
			where m.recipient.id = :userId and m.readAt is null group by m.sender.id
			""")
	List<Object[]> countUnreadBySender(@Param("userId") Long userId);

	long countByRecipientIdAndReadAtIsNull(Long recipientId);

	@Modifying
	@Query("""
			update Message m set m.readAt = :now
			where m.sender.id = :senderId and m.recipient.id = :recipientId and m.readAt is null
			""")
	int markRead(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId,
			@Param("now") LocalDateTime now);
}
