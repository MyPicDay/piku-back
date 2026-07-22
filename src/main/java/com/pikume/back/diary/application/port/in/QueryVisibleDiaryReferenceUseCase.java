package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.VisibleDiaryReferenceView;

import java.util.Optional;

public interface QueryVisibleDiaryReferenceUseCase {
	Optional<VisibleDiaryReferenceView> queryVisibleDiaryReference(Long diaryId, String viewerId);
}
