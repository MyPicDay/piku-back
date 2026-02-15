package store.piku.back.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import store.piku.back.user.application.dto.ProfilePreviewResult;

@Schema(description = "프로필 미리보기 응답")
public record ProfilePreviewResponse(
		@Schema(description = "사용자 ID") String id,
		@Schema(description = "닉네임") String nickname,
		@Schema(description = "아바타 이미지 URL") String avatar,
		@Schema(description = "친구 수") int friendCount,
		@Schema(description = "일기 총 개수") long diaryCount,
		@Schema(description = "친구 관계 상태") String friendStatus) {
	public static ProfilePreviewResponse from(ProfilePreviewResult result) {
		return new ProfilePreviewResponse(
				result.id(),
				result.nickname(),
				result.avatar(),
				result.friendCount(),
				result.diaryCount(),
				result.friendStatus());
	}
}
