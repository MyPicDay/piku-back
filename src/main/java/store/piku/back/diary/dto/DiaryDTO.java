package store.piku.back.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.enums.DiaryPhotoType;
import store.piku.back.diary.enums.Status;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryDTO {
    @NotNull
    @Schema(description = "공개범위")
    private Status status;

    @NotBlank(message = "일기 내용은 비어 있을 수 없습니다.")
    @Schema(description = "일기 내용")
    private String content;

    @Size(max = 3, message = "AI 사진은 최대 3개까지 업로드할 수 있습니다.")
    @Schema(description = "AI 사진 ( 값을 넣지 않을 시에 'Send empty value' 클릭 하지 마세요 ) ")
    private List<Long> aiPhotos;

    @Schema(description = "내 앨범 사진 ( 값을 넣지 않을 시에 'Send empty value' 클릭 하지 마세요 )", nullable = true)
    private List<MultipartFile> photos;

    @Schema(description = "일기날짜 ")
    private LocalDate date;

    @Schema(description = "대표 사진의 타입 ( 값을 넣지 않을 시에 'Send empty value' 클릭 하지 마세요 ) ")
    private DiaryPhotoType coverPhotoType;

    @Schema(description = "대표 사진의 결정을 위한 사진 번호 ( 값을 넣지 않을 시에 'Send empty value' 클릭 하지 마세요 )")
    private Integer  coverPhotoIndex;
}
