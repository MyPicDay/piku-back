package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.port.in.AttachGeneratedImageToDiaryUseCase;
import com.pikume.back.creative.application.port.in.QueryDiaryImageGenerationUseCase;
import com.pikume.back.creative.application.port.in.UpdateGeneratedImagePathUseCase;
import com.pikume.back.diary.application.port.out.ManageGeneratedImageForDiaryPort;

@Component
@RequiredArgsConstructor
public class CreativeAdapterForDiary implements ManageGeneratedImageForDiaryPort {

	private final QueryDiaryImageGenerationUseCase queryDiaryImageGenerationUseCase;
	private final AttachGeneratedImageToDiaryUseCase attachGeneratedImageToDiaryUseCase;
	private final UpdateGeneratedImagePathUseCase updateGeneratedImagePathUseCase;

	@Override
	public String loadGeneratedImagePath(Long generationId) {
		return queryDiaryImageGenerationUseCase.queryGenerationForDiary(generationId).filePath();
	}

	@Override
	public boolean isGeneratedImageAvailableForDiary(Long generationId, String userId) {
		return queryDiaryImageGenerationUseCase.isGenerationAvailableForDiary(generationId, userId);
	}

	@Override
	public void updateGeneratedImagePath(Long generationId, String filePath) {
		updateGeneratedImagePathUseCase.updateGeneratedImagePath(generationId, filePath);
	}

	@Override
	public void attachGeneratedImageToDiary(Long generationId, Long diaryId) {
		attachGeneratedImageToDiaryUseCase.attachGenerationToDiary(generationId, diaryId);
	}
}
