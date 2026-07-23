package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.port.in.AttachGeneratedImageToDiaryUseCase;
import com.pikume.back.creative.application.port.in.QueryDiaryImageGenerationUseCase;
import com.pikume.back.creative.application.port.in.UpdateGeneratedImagePathUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreativeAdapterForDiary")
class CreativeAdapterForDiaryTest {

	@Mock
	private QueryDiaryImageGenerationUseCase queryDiaryImageGenerationUseCase;

	@Mock
	private AttachGeneratedImageToDiaryUseCase attachGeneratedImageToDiaryUseCase;

	@Mock
	private UpdateGeneratedImagePathUseCase updateGeneratedImagePathUseCase;

	@Test
	@DisplayName("Creative 목적별 공개 계약을 Diary 생성 이미지 Port로 번역한다")
	void translatesCreativeUseCasesForDiary() {
		CreativeAdapterForDiary adapter = adapter();
		given(queryDiaryImageGenerationUseCase.queryGenerationForDiary(1L))
				.willReturn(new DiaryImageGenerationView(1L, "user-1", "prompt", "private/a.png", null));
		given(queryDiaryImageGenerationUseCase.isGenerationAvailableForDiary(1L, "user-1"))
				.willReturn(true);

		assertThat(adapter.loadGeneratedImagePath(1L)).isEqualTo("private/a.png");
		assertThat(adapter.isGeneratedImageAvailableForDiary(1L, "user-1")).isTrue();

		adapter.updateGeneratedImagePath(1L, "public/a.png");
		adapter.attachGeneratedImageToDiary(1L, 2L);

		then(updateGeneratedImagePathUseCase).should().updateGeneratedImagePath(1L, "public/a.png");
		then(attachGeneratedImageToDiaryUseCase).should().attachGenerationToDiary(1L, 2L);
	}

	private CreativeAdapterForDiary adapter() {
		return new CreativeAdapterForDiary(
				queryDiaryImageGenerationUseCase,
				attachGeneratedImageToDiaryUseCase,
				updateGeneratedImagePathUseCase);
	}
}
