package com.pikume.back.user.application.dto;

import java.util.List;

/**
 * 사용자 프로필 상세 결과 DTO
 */
public record UserProfileResult(
		String id,
		String nickname,
		UserAvatarReference avatarReference,
		int friendCount,
		long diaryCount,
		String friendStatus,
		boolean isOwner,
		List<MonthlyDiaryCount> monthlyDiaryCount) {

	public record MonthlyDiaryCount(int year, int month, long count) {
	}
}
