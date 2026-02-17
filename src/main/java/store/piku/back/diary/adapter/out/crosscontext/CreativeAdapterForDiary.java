package store.piku.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.creative.application.port.in.ManageGenerationUseCase;
import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.diary.application.port.out.LoadCreativePort;

@Component
@RequiredArgsConstructor
public class CreativeAdapterForDiary implements LoadCreativePort {

	private final ManageGenerationUseCase manageGenerationUseCase;

	@Override
	public DiaryImageGeneration findById(Long id) {
		return manageGenerationUseCase.findById(id);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return manageGenerationUseCase.existsByIdAndUserId(id, userId);
	}

	@Override
	public void updateDiaryId(Long generationId, Long diaryId) {
		manageGenerationUseCase.updateDiaryId(generationId, diaryId);
	}
}
