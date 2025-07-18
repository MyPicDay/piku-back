package store.piku.back.notification.dto;

import store.piku.back.notification.entity.NotificationType;

public class NotificationDTO {
    private Long id;
    private Long receiverId;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private Long relatedId;
}
