package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.port.in.ManageGenerationUseCase;
import com.pikume.back.diary.application.port.out.LoadCreativePort;

@Component
@RequiredArgsConstructor
public class CreativeAdapterForDiary implements LoadCreativePort {

	private final ManageGenerationUseCase manageGenerationUseCase;

	@Override
	public DiaryImageGenerationView findById(Long id) {
		return manageGenerationUseCase.findById(id);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return manageGenerationUseCase.existsByIdAndUserId(id, userId);
	}

	@Override
	public void updateFilePath(Long generationId, String filePath) {
		manageGenerationUseCase.updateFilePath(generationId, filePath);
	}

	@Override
	public void updateDiaryId(Long generationId, Long diaryId) {
		manageGenerationUseCase.updateDiaryId(generationId, diaryId);
	}
}
