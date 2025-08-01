package store.piku.back.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.notification.entity.Notification;
import store.piku.back.notification.entity.NotificationType;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId);
    long countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(String userId);
}
