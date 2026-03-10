package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.VisibleDiaryView;

import java.util.Optional;

public interface QueryDiaryVisibilityUseCase {

	Optional<VisibleDiaryView> findVisibleDiaryById(Long diaryId, String viewerId);

	boolean existsVisibleById(Long diaryId, String viewerId);

	Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId);
}
