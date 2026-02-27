package com.pikume.back.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pikume.back.user.application.dto.UpdateProfileResult;

@Schema(description = "닉네임/캐릭터 변경 응답")
public record NicknameChangeResponse(
		@Schema(description = "성공 여부") boolean success,
		@Schema(description = "메시지") String message,
		@Schema(description = "새 닉네임") String newNickname,
		@Schema(description = "아바타") String avatar) {
	public static NicknameChangeResponse from(UpdateProfileResult result) {
		return new NicknameChangeResponse(
				result.success(),
				result.message(),
				result.newNickname(),
				result.avatar());
	}
}
