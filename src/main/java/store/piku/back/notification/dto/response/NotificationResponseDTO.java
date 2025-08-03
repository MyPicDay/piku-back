package store.piku.back.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.diary.entity.Diary;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.user.entity.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponseDTO {

    @Schema(description = "알림 ID")
    private Long id;

    @Schema(description = "알림 메세지")
    private String message;

    private String nickname;

    private String avatarUrl;

    private NotificationType type;

    @Schema(description = "해당 알림 일기 ID")
    private Long relatedDiaryId;

    private String thumbnailUrl;
}
