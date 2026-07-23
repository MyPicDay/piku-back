package com.pikume.back.creative.application.port.in;

public interface AttachGeneratedImageToDiaryUseCase {

	void attachGenerationToDiary(Long generationId, Long diaryId);
}
