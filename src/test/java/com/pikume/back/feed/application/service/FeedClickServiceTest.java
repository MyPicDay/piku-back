package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadFeedClickHistoryPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryDetailPort;
import com.pikume.back.feed.application.port.out.RecordFeedClickPort;
import com.pikume.back.feed.application.port.out.RecordFeedClickPreferencePort;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.domain.FeedClick;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedClickService")
class FeedClickServiceTest {

	@Mock
	private LoadFeedDiaryDetailPort loadFeedDiaryDetailPort;
	@Mock
	private LoadFeedClickHistoryPort loadFeedClickHistoryPort;
	@Mock
	private RecordFeedClickPort recordFeedClickPort;
	@Mock
	private RecordFeedClickPreferencePort recordFeedClickPreferencePort;

	private FeedClickService service;

	@BeforeEach
	void setUp() {
		service = new FeedClickService(
				loadFeedDiaryDetailPort,
				loadFeedClickHistoryPort,
				recordFeedClickPort,
				recordFeedClickPreferencePort);
	}

	@Nested
	@DisplayName("recordClick")
	class RecordClick {

		@Test
		@DisplayName("처음 클릭하면 클릭을 저장한 뒤 같은 흐름에서 선호도를 기록한다")
		void recordsClickBeforePreference() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "user-id"))
					.willReturn(Optional.of(visibleDiary()));
			given(loadFeedClickHistoryPort.hasClick("user-id", 1L)).willReturn(false);

			service.recordClick("user-id", 1L);

			InOrder order = inOrder(recordFeedClickPort, recordFeedClickPreferencePort);
			order.verify(recordFeedClickPort).record(any(FeedClick.class));
			order.verify(recordFeedClickPreferencePort).recordClickPreference("user-id", 1L);
		}

		@Test
		@DisplayName("이미 클릭한 경우 중복 저장하거나 선호도를 기록하지 않는다")
		void ignoresDuplicateClick() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "user-id"))
					.willReturn(Optional.of(visibleDiary()));
			given(loadFeedClickHistoryPort.hasClick("user-id", 1L)).willReturn(true);

			service.recordClick("user-id", 1L);

			then(recordFeedClickPort).shouldHaveNoInteractions();
			then(recordFeedClickPreferencePort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("보이지 않는 일기는 클릭을 기록하지 않는다")
		void ignoresInvisibleDiary() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "user-id"))
					.willReturn(Optional.empty());

			service.recordClick("user-id", 1L);

			then(loadFeedClickHistoryPort).shouldHaveNoInteractions();
			then(recordFeedClickPort).shouldHaveNoInteractions();
			then(recordFeedClickPreferencePort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("선호도 기록 실패는 저장된 클릭 흐름에 전파하지 않는다")
		void swallowsPreferenceFailure() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "user-id"))
					.willReturn(Optional.of(visibleDiary()));
			given(loadFeedClickHistoryPort.hasClick("user-id", 1L)).willReturn(false);
			org.mockito.BDDMockito.willThrow(new IllegalStateException("preference failed"))
					.given(recordFeedClickPreferencePort)
					.recordClickPreference("user-id", 1L);

			assertThatCode(() -> service.recordClick("user-id", 1L))
					.doesNotThrowAnyException();

			then(recordFeedClickPort).should().record(any(FeedClick.class));
		}
	}

	private FeedDiaryDetailView visibleDiary() {
		return new FeedDiaryDetailView(
				1L,
				"writer-id",
				FeedVisibility.PUBLIC,
				"content",
				List.of(),
				LocalDate.of(2026, 3, 8),
				LocalDateTime.of(2026, 3, 8, 10, 0));
	}
}
