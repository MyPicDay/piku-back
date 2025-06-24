package store.piku.back.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.diary.enums.Status;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "일기 조회 응답DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
    private Long diaryId;

    @Schema(description = "일기 공개범위")
    private Status status;
    private String content;

    @Schema(description = "일기 사진 ( AI + 일반 사진 ) ")
    private List<String> imgUrls;  // 업로드용
    private LocalDate date;
    private String nickname; // 작성자 닉네임 추가

    @Schema(description = "사용자 프로필 사진")
    private String avatar;

    private String userId;


}