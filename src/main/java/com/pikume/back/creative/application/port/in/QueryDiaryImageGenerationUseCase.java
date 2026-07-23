package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.DiaryImageGenerationView;

public interface QueryDiaryImageGenerationUseCase {

	DiaryImageGenerationView queryGenerationForDiary(Long generationId);

	boolean isGenerationAvailableForDiary(Long generationId, String userId);
}
