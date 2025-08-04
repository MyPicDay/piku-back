package store.piku.back.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.notification.entity.NotificationType;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseResponse {

    private NotificationType type;
    private String message;
    private Long relatedId;
    private String senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String thumbnailUrl;

}

