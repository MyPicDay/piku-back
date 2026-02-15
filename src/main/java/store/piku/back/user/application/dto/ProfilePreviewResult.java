package store.piku.back.user.application.dto;

/**
 * 프로필 미리보기 결과 DTO
 */
public record ProfilePreviewResult(
		String id,
		String nickname,
		String avatar,
		int friendCount,
		long diaryCount,
		String friendStatus) {
}
