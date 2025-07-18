package store.piku.back.notification.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import store.piku.back.global.entity.BaseEntity;

public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long receiverId;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private Long relatedId;
}
