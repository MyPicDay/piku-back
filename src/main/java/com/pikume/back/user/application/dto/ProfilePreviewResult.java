package com.pikume.back.user.application.dto;

/**
 * 프로필 미리보기 결과 DTO
 */
public record ProfilePreviewResult(
		String id,
		String nickname,
		UserAvatarReference avatarReference,
		int friendCount,
		long diaryCount,
		String friendStatus) {
}
