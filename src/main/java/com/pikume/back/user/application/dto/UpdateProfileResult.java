package com.pikume.back.user.application.dto;

/**
 * 프로필 변경 결과 DTO
 */
public record UpdateProfileResult(
		boolean success,
		String message,
		String newNickname,
		String avatar) {
	public static UpdateProfileResult success(String message, String nickname, String avatar) {
		return new UpdateProfileResult(true, message, nickname, avatar);
	}

	public static UpdateProfileResult failure(String message, String nickname) {
		return new UpdateProfileResult(false, message, nickname, null);
	}
}
