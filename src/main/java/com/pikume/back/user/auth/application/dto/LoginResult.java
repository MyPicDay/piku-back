package com.pikume.back.user.auth.application.dto;

public record LoginResult(String accessToken, String refreshToken, UserInfo userInfo) {
	public record UserInfo(String id, String nickname, String avatarPath) {
	}
}
