package store.piku.back.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import store.piku.back.diary.enums.DiaryPhotoType;

@Data
@AllArgsConstructor
@Schema(description = "일기 이미지 정보 DTO")
public class DiaryImageInfo {
    @Schema(description = "이미지 타입 (AI 또는 UPLOAD)")
    @NotNull(message = "이미지 타입은 필수입니다.")
    private DiaryPhotoType type;

    @Schema(description = "이미지 순서 (0부터 시작)")
    @NotNull(message = "이미지 순서는 필수입니다.")
    private Integer order;

    @Positive(message = "AI 사진 ID는 양수여야 합니다.")
    @Schema(description = "AI 사진 ID (type이 AI일 경우)")
    private Long aiPhotoId;

    @Schema(description = "업로드한 사진의 인덱스 (type이 UPLOAD일 경우, photos 리스트의 인덱스)")
    private Integer photoIndex;
}
