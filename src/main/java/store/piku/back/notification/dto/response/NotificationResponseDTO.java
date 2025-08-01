package store.piku.back.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponseDTO {

    @Schema(description = "알림 ID")
    private Long id;

    @Schema(description = "관련된 엔티티의 ID ( 예 : 댓글 ID , 친구 ID 등 ) ")
    private String relatedId;

    @Schema(description = "알림 읽음 여부")
    private Boolean isRead;

    @Schema(description = "알림 수신자 ID")
    private String receiverId;

    @Schema(description = "알림 메세지")
    private String message;

    @Schema(description = "대표사진 URL ( 예: 친구 프로필 사진 과 일기대표사진 에 사용됨 ) ")
    private String thumbnailUrl;
}
