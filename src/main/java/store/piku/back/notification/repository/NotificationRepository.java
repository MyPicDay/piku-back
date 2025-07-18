package store.piku.back.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.notification.dto.NotificationDTO;
import store.piku.back.notification.entity.Notification;
import store.piku.back.user.entity.User;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<NotificationDTO> findAllByUser(User user);
}
