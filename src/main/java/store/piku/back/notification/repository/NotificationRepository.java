package store.piku.back.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.piku.back.notification.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(String userId);

    List<Notification> findAllByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(String receiverId);

    Page<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId, Pageable pageable);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.receiverId = :receiverId AND n.type = 'FRIEND_REQUEST'")
    boolean existsFriendRequestByReceiverId(@Param("receiverId") String receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false AND n.deletedAt IS NULL")
    int markAllAsReadByReceiverId(@Param("receiverId") String receiverId);

}
