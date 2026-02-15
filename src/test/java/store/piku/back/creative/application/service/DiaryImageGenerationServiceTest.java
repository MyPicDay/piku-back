package store.piku.back.creative.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.creative.application.port.out.LoadGenerationPort;
import store.piku.back.creative.application.port.out.SaveGenerationPort;
import store.piku.back.creative.domain.DiaryImageGeneration;

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
	private SaveGenerationPort savePort;

	@Nested
	@DisplayName("updateDiaryId")
	class UpdateDiaryId {

		@Test
		@DisplayName("일기 ID를 정상 업데이트한다")
		void updatesDiaryIdSuccessfully() {
			Long historyId = 1L;
			Long diaryId = 100L;
			DiaryImageGeneration generation = new DiaryImageGeneration("user-1", "prompt", "path");
			given(loadPort.findById(historyId)).willReturn(Optional.of(generation));

			service.updateDiaryId(historyId, diaryId);

			assertThat(generation.getDiaryId()).isEqualTo(diaryId);
			then(savePort).should().save(generation);
		}
	}

	@Nested
	@DisplayName("getByUserIdAndFilePath")
	class GetByUserIdAndFilePath {

		@Test
		@DisplayName("사용자 ID와 경로로 조회 성공")
		void returnsGeneration() {
			String userId = "user-1";
			String path = "path/image.png";
			DiaryImageGeneration generation = new DiaryImageGeneration(userId, "prompt", path);
			given(loadPort.findByUserIdAndFilePath(userId, path)).willReturn(Optional.of(generation));

			DiaryImageGeneration result = service.getByUserIdAndFilePath(userId, path);

			assertThat(result).isEqualTo(generation);
		}

		@Test
		@DisplayName("조회 실패 시 예외 발생")
		void throwsWhenNotFound() {
			given(loadPort.findByUserIdAndFilePath(anyString(), anyString())).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.getByUserIdAndFilePath("user-1", "path"))
					.isInstanceOf(RuntimeException.class);
		}
	}
}
