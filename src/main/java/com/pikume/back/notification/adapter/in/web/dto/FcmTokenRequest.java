package com.pikume.back.notification.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

	@Schema(description = "사용자 ID")
	private String userId;

	@Schema(description = "FCM 토큰")
	private String token;

	@Schema(description = "기기 고유 ID")
	private String deviceId;
}
