package com.pikume.back.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 중복 확인 응답")
public record NicknameCheckResponse(
		@Schema(description = "허용 or 거절", example = "true or false") boolean success,
		@Schema(description = "확인 메시지") String message) {
}
