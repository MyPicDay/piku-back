package com.pikume.back.creative.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.creative.application.port.out.RecordGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryImageGenerationService")
class DiaryImageGenerationServiceTest {

	@InjectMocks
	private DiaryImageGenerationService service;

	@Mock
	private LoadGenerationPort loadPort;

	@Mock
	private RecordGenerationPort recordPort;

	@Nested
	@DisplayName("attachGenerationToDiary")
	class AttachGenerationToDiary {

		@Test
		@DisplayName("일기 ID를 정상 업데이트한다")
		void updatesDiaryIdSuccessfully() {
			Long historyId = 1L;
			Long diaryId = 100L;
			DiaryImageGeneration generation = new DiaryImageGeneration("user-1", "prompt", "path");
			given(loadPort.loadGenerationForDiaryIntegration(historyId)).willReturn(Optional.of(generation));

			service.attachGenerationToDiary(historyId, diaryId);

			assertThat(generation.getDiaryId()).isEqualTo(diaryId);
			then(recordPort).should().recordGeneration(generation);
		}

		@Test
		@DisplayName("이미 다른 일기에 연결된 생성 이력은 덮어쓰지 않는다")
		void rejectsGenerationAlreadyAttachedToDiary() {
			Long historyId = 1L;
			DiaryImageGeneration generation = new DiaryImageGeneration("user-1", "prompt", "path");
			generation.attachToDiary(99L);
			given(loadPort.loadGenerationForDiaryIntegration(historyId)).willReturn(Optional.of(generation));

			assertThatThrownBy(() -> service.attachGenerationToDiary(historyId, 100L))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("이미");

			assertThat(generation.getDiaryId()).isEqualTo(99L);
			then(recordPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("폐기된 생성 이력은 일기에 연결하지 않는다")
		void rejectsDiscardedGeneration() {
			Long historyId = 1L;
			DiaryImageGeneration generation = new DiaryImageGeneration("user-1", "prompt", "path");
			generation.discard();
			given(loadPort.loadGenerationForDiaryIntegration(historyId)).willReturn(Optional.of(generation));

			assertThatThrownBy(() -> service.attachGenerationToDiary(historyId, 100L))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("폐기");

			assertThat(generation.getDiaryId()).isNull();
			then(recordPort).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("loadGenerationForDiary")
	class LoadGenerationForDiary {

		@Test
		@DisplayName("Diary 연동을 위한 생성 이력을 조회한다")
		void returnsGeneration() {
			Long generationId = 1L;
			String path = "path/image.png";
			DiaryImageGeneration generation = new DiaryImageGeneration("user-1", "prompt", path);
			given(loadPort.loadGenerationForDiaryIntegration(generationId)).willReturn(Optional.of(generation));

			DiaryImageGenerationView result = service.loadGenerationForDiary(generationId);

			assertThat(result.filePath()).isEqualTo(path);
			assertThat(result.userId()).isEqualTo("user-1");
		}

		@Test
		@DisplayName("조회 실패 시 예외 발생")
		void throwsWhenNotFound() {
			given(loadPort.loadGenerationForDiaryIntegration(1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.loadGenerationForDiary(1L))
					.isInstanceOf(RuntimeException.class);
		}
	}
}
