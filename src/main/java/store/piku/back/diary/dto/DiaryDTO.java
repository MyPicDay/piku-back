package store.piku.back.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @NotNull
    @Schema(description = "일기 이미지 정보들")
    private List<DiaryImageInfo> imageInfos;

    @NotNull
    @Schema(description = "일기날짜 ")
    private LocalDate date;
}
