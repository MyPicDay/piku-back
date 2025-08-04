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

    @Schema(description = "보낸 사람 닉네임")
    private String nickname;

    @Schema(description = "보낸 사람 프로필 사진")
    private String avatarUrl;

    @Schema(description = "알림 유형")
    private NotificationType type;

    @Schema(description = "해당 알림 일기 ID")
    private Long relatedDiaryId;

    @Schema(description = "해당 일기 대표 사진")
    private String thumbnailUrl;

    @Schema(description = "읽음 여부")
    private Boolean isRead;
}
