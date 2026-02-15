package store.piku.back.user.application.dto;

import store.piku.back.user.application.port.out.UserDiaryPort;

import java.util.List;

/**
 * 사용자 프로필 상세 결과 DTO
 */
public record UserProfileResult(
		String id,
		String nickname,
		String avatar,
		int friendCount,
		long diaryCount,
		String friendStatus,
		boolean isOwner,
		List<UserDiaryPort.MonthlyDiaryCount> monthlyDiaryCount) {
}
