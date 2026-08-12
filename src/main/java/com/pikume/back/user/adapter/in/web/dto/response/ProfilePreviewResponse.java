package com.pikume.back.user.adapter.in.web.dto.response;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserAvatarReference;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pikume.back.user.application.dto.ProfilePreviewResult;

@Schema(description = "프로필 미리보기 응답")
public record ProfilePreviewResponse(
		@Schema(description = "사용자 ID") String id,
		@Schema(description = "닉네임") String nickname,
		@Schema(description = "아바타 이미지 URL") String avatar,
		@Schema(description = "친구 수") int friendCount,
		@Schema(description = "일기 총 개수") long diaryCount,
		@Schema(description = "친구 관계 상태") String friendStatus) {
	public static ProfilePreviewResponse from(ProfilePreviewResult result, ResolveObjectUrlPort objectUrlPort) {
		return new ProfilePreviewResponse(
				result.id(),
				result.nickname(),
				resolveAvatarUrl(result.avatarReference(), objectUrlPort),
				result.friendCount(),
				result.diaryCount(),
				result.friendStatus());
	}

	private static String resolveAvatarUrl(
			UserAvatarReference reference,
			ResolveObjectUrlPort objectUrlPort) {
		if (reference == null) {
			return null;
		}
		if (reference.absoluteUrl()) {
			return reference.value();
		}
		return objectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
