package store.piku.back.diary.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.social.domain.friend.vo.FriendStatus;
import store.piku.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "일기 조회 응답DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {
	private Long diaryId;

	@Schema(description = "일기 공개범위")
	private DiaryVisibility status;
	private String content;

	@Schema(description = "일기 사진 ( AI + 일반 사진 ) ")
	private List<String> imgUrls;
	private LocalDate date;
	private String nickname;

	@Schema(description = "사용자 프로필 사진")
	private String avatar;

	private String userId;

	private LocalDateTime createdAt;

	private FriendStatus friendStatus;

	private Long commentCount;

	@Schema(description = "좋아요 수")
	private Long likeCount;

	@Schema(description = "현재 사용자의 좋아요 여부")
	private Boolean isLiked;
}
