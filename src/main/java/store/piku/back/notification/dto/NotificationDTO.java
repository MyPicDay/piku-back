package store.piku.back.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.notification.entity.NotificationType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long receiverId;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private Long relatedId;
}
