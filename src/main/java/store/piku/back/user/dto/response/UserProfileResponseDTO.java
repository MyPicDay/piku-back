package store.piku.back.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import store.piku.back.diary.dto.DiaryMonthCountDTO;
import store.piku.back.diary.enums.FriendStatus;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "사용자 프로필 응답 DTO")
public class UserProfileResponseDTO {

    @Schema(description = "사용자 ID")
    private String id;

    @Schema(description = "닉네임")
    private String nickname;

    @Schema(description = "아바타 이미지 URL")
    private String avatar;

    @Schema(description = "친구 수")
    private int friendCount;

    @Schema(description = "일기 총 개수")
    private long diaryCount;

    @Schema(description = "친구 관계 상태")
    private FriendStatus friendStatus;

    @Schema(description = "본인 프로필 여부")
    private boolean isOwner;

    @Schema(description = "월별 일기 개수 리스트")
    private List<DiaryMonthCountDTO> monthlyDiaryCount;

}
