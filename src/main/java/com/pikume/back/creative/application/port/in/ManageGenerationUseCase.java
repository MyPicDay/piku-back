package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.DiaryImageGenerationView;

/**
 * 생성 이력 관리 Inbound Port
 */
public interface ManageGenerationUseCase {

	DiaryImageGenerationView loadGenerationForDiary(Long generationId);

	void attachGenerationToDiary(Long generationId, Long diaryId);

	void updateGeneratedImagePath(Long generationId, String filePath);

	boolean isGenerationAvailableForDiary(Long generationId, String userId);
}
