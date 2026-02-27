package com.pikume.back.user.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 변경 요청")
public record UpdateProfileRequest(
		@Schema(description = "변경할 닉네임") String newNickname,

		@Schema(description = "변경할 캐릭터 ID") Long characterId) {
}
