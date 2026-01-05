package store.piku.back.like.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "좋아요 응답 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponse {

    @Schema(description = "일기 ID")
    private Long diaryId;

    @Schema(description = "좋아요 수")
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부")
    private boolean isLiked;
}
