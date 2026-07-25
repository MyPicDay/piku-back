package com.pikume.back.notification.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.notification.domain.Notification;

import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

	long countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(String userId);

	Page<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId, Pageable pageable);

	Optional<Notification> findByIdAndDeletedAtIsNull(Long notificationId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Notification n
			SET n.isRead = true,
				n.updatedAt = CURRENT_TIMESTAMP
			WHERE n.id = :notificationId
				AND n.receiverId = :receiverId
				AND n.deletedAt IS NULL
				AND n.isRead = false
			""")
	int markAsReadIfActive(
			@Param("notificationId") Long notificationId,
			@Param("receiverId") String receiverId);

	@Query("""
			SELECT COUNT(n) > 0
			FROM Notification n
			WHERE n.receiverId = :receiverId
				AND n.type = 'FRIEND_REQUEST'
				AND n.deletedAt IS NULL
			""")
	boolean existsActiveFriendRequestByReceiverId(@Param("receiverId") String receiverId);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false AND n.deletedAt IS NULL")
	int markAllAsReadByReceiverId(@Param("receiverId") String receiverId);

	@Modifying
	@Query("UPDATE Notification n SET n.deletedAt = CURRENT_TIMESTAMP WHERE n.diaryId = :diaryId AND n.deletedAt IS NULL")
	int deleteByDiaryId(@Param("diaryId") Long diaryId);
}
