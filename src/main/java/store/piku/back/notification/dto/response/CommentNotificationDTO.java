package store.piku.back.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentNotificationDTO {

    private String relatedId;
    private Boolean isRead;
    private String receiverId;
    private String message;
    private String thumbnailUrl;
}
