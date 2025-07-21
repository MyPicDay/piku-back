package store.piku.back.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.notification.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findAllByReceiverId(String receiverId);
    List<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId);

}
