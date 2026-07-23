package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationAdapterForDiary")
class RecommendationAdapterForDiaryTest {

	@Mock
	private AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

	@Test
	@DisplayName("Diary 분석 요청을 Recommendation 공개 계약으로 번역한다")
	void delegatesDiaryAnalysis() {
		RecommendationAdapterForDiary adapter =
				new RecommendationAdapterForDiary(analyzeDiaryContentUseCase);

		adapter.analyze(1L, "일기 내용");

		then(analyzeDiaryContentUseCase).should()
				.analyzeDiaryContent(1L, "일기 내용");
	}
}
