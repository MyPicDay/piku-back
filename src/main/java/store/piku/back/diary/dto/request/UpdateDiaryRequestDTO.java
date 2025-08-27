package store.piku.back.diary.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.diary.dto.response.DiaryImageInfo;
import store.piku.back.diary.enums.Status;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일기 수정 요청 DTO ")
public class UpdateDiaryRequestDTO {

    @NotNull
    @Schema(description = "공개범위")
    private Long diaryId ;

    @NotBlank
    @Schema(description = "일기 내용")
    private String content;

    @NotNull
    @Schema(description = "공개범위")
    private Status status;

    @NotNull
    @Schema(description = "일기 이미지 정보들")
    private List<DiaryImageInfo> imageInfo;


}
