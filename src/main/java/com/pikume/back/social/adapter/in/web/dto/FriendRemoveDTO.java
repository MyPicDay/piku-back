package com.pikume.back.social.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Schema(description = "친구 관계 삭제 결과 DTO")
public class FriendRemoveDTO {

	@Schema(description = "성공 여부", example = "true")
	private boolean success;
	@Schema(description = "결과 메시지", example = "친구 관계가 해제되었습니다.")
	private String message;
}
