package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.diary.application.port.out.AnalyzeDiaryContentPort;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationAdapterForDiary implements AnalyzeDiaryContentPort {

	private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

	@Override
	public void analyze(Long diaryId, String content) {
		analyzeDiaryContentUseCase.analyzeAndSave(diaryId, content);
	}
}
