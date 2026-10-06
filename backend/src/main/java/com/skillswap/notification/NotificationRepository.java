package com.skillswap.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query(value = """
			select n from Notification n left join fetch n.actor
			where n.recipient.id = :userId and (:unreadOnly = false or n.read = false)
			order by n.createdAt desc, n.id desc
			""", countQuery = """
			select count(n) from Notification n
			where n.recipient.id = :userId and (:unreadOnly = false or n.read = false)
			""")
	Page<Notification> findForUser(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly,
			Pageable pageable);

	long countByRecipientIdAndReadFalse(Long recipientId);

	boolean existsByRecipientIdAndActorIdAndTypeAndReadFalse(Long recipientId, Long actorId, NotificationType type);

	@Modifying
	@Query("update Notification n set n.read = true where n.recipient.id = :userId and n.read = false")
	int markAllRead(@Param("userId") Long userId);
}
