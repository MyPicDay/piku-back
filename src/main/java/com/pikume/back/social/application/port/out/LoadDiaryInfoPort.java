package com.pikume.back.social.application.port.out;

import java.util.Optional;

/**
 * Diary Context에서 일기 정보를 조회하는 cross-context port.
 */
public interface LoadDiaryInfoPort {

	record DiaryInfo(
			Long diaryId,
			String ownerUserId,
			boolean anonymous,
			boolean viewerOwner
	) {
	}

	boolean existsVisibleById(Long diaryId, String viewerId);

	/**
	 * 일기의 소유자 userId를 반환합니다.
	 */
	Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId);

	Optional<DiaryInfo> findVisibleDiaryInfoByDiaryId(Long diaryId, String viewerId);
}
