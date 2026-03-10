package com.pikume.back.social.application.port.out;

import java.util.Optional;

/**
 * Diary Context에서 일기 정보를 조회하는 cross-context port.
 */
public interface LoadDiaryInfoPort {

	boolean existsVisibleById(Long diaryId, String viewerId);

	/**
	 * 일기의 소유자 userId를 반환합니다.
	 */
	Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId);
}
