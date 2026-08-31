package com.pikume.back.user.adapter.in.web.dto.response;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pikume.back.user.application.dto.UpdateProfileResult;

@Schema(description = "닉네임/캐릭터 변경 응답")
public record NicknameChangeResponse(
		@Schema(description = "성공 여부") boolean success,
		@Schema(description = "메시지") String message,
		@Schema(description = "새 닉네임") String newNickname,
		@Schema(description = "아바타 이미지 URL") String avatar) {
	public static NicknameChangeResponse from(UpdateProfileResult result, ResolveObjectUrlPort objectUrlPort) {
		return new NicknameChangeResponse(
				result.success(),
				result.message(),
				result.newNickname(),
				resolveAvatarUrl(result.avatarReference(), objectUrlPort));
	}

	private static String resolveAvatarUrl(String avatarReference, ResolveObjectUrlPort objectUrlPort) {
		if (avatarReference == null || avatarReference.startsWith("http://") || avatarReference.startsWith("https://")) {
			return avatarReference;
		}
		return objectUrlPort.resolveObjectUrl(avatarReference, true);
	}
}
