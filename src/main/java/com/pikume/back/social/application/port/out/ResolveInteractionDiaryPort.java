package com.pikume.back.social.application.port.out;

import com.pikume.back.social.application.readmodel.InteractionDiaryView;

import java.util.Optional;

public interface ResolveInteractionDiaryPort {
	Optional<InteractionDiaryView> resolveVisibleDiary(Long diaryId, String viewerId);

	default boolean visibleDiaryExists(Long diaryId, String viewerId) {
		return resolveVisibleDiary(diaryId, viewerId).isPresent();
	}
}
