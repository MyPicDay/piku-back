package store.piku.back.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.notification.entity.NotificationType;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseResponse {

    @Schema(description = "알림 유형")
    private NotificationType type;

    @Schema(description = "알림 메세지")
    private String message;

    @Schema(description = "일기 ID")
    private Long relatedId;

    @Schema(description = "보낸 사람 ID")
    private String senderId;

    @Schema(description = "보낸 사람 닉네임")
    private String senderNickname;

    @Schema(description = "보낸사람 프로필 사진")
    private String senderAvatarUrl;

    @Schema(description = "해당 일기 대표 사진")
    private String thumbnailUrl;

}

