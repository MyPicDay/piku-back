package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.port.in.ManageGenerationUseCase;
import com.pikume.back.diary.application.port.out.ManageGeneratedImageForDiaryPort;

@Component
@RequiredArgsConstructor
public class CreativeAdapterForDiary implements ManageGeneratedImageForDiaryPort {

	private final ManageGenerationUseCase manageGenerationUseCase;

	@Override
	public String loadGeneratedImagePath(Long generationId) {
		return manageGenerationUseCase.loadGenerationForDiary(generationId).filePath();
	}

	@Override
	public boolean isGeneratedImageAvailableForDiary(Long generationId, String userId) {
		return manageGenerationUseCase.isGenerationAvailableForDiary(generationId, userId);
	}

	@Override
	public void updateGeneratedImagePath(Long generationId, String filePath) {
		manageGenerationUseCase.updateGeneratedImagePath(generationId, filePath);
	}

	@Override
	public void attachGeneratedImageToDiary(Long generationId, Long diaryId) {
		manageGenerationUseCase.attachGenerationToDiary(generationId, diaryId);
	}
}
