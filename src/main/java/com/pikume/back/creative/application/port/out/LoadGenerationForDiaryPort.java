package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.util.Optional;

public interface LoadGenerationForDiaryPort {

	Optional<DiaryImageGeneration> loadGenerationForDiary(Long generationId);

	boolean isGenerationAvailableForDiary(Long generationId, String userId);
}
