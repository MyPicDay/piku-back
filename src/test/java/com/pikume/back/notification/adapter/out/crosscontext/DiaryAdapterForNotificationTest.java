package com.pikume.back.notification.adapter.out.crosscontext;

import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryAdapterForNotification")
class DiaryAdapterForNotificationTest {

	@InjectMocks
	private DiaryAdapterForNotification adapter;

	@Mock
	private QueryDiaryReadUseCase queryDiaryReadUseCase;
	@Mock
	private ResolveObjectUrlPort resolveObjectUrlPort;

	@Test
	@DisplayName("Diary 공개 계약을 Notification 소유 Context로 일괄 번역한다")
	void loadsDiaryContexts() {
		Set<Long> diaryIds = Set.of(10L, 20L);
		given(queryDiaryReadUseCase.getDiarySummaries(diaryIds))
				.willReturn(Map.of(
						10L,
						new DiarySummaryView(
								10L,
								"owner-id",
								DiaryVisibility.ANONYMOUS,
								"content",
								LocalDate.of(2026, 7, 20),
								LocalDateTime.of(2026, 7, 20, 12, 0))));
		given(queryDiaryReadUseCase.getRepresentPhotoPaths(diaryIds))
				.willReturn(Map.of(10L, "photos/10.jpg"));
		given(resolveObjectUrlPort.resolveObjectUrl("photos/10.jpg", false))
				.willReturn("thumbnail-url");

		Map<Long, NotificationDiaryContextView> result =
				adapter.loadNotificationDiaryContexts(diaryIds);

		assertThat(result).containsOnlyKeys(10L);
		NotificationDiaryContextView context = result.get(10L);
		assertThat(context.thumbnailUrl()).isEqualTo("thumbnail-url");
		assertThat(context.diaryUserId()).isEqualTo("owner-id");
		assertThat(context.anonymous()).isTrue();
		assertThat(context.diaryDate()).isNull();
	}

	@Test
	@DisplayName("대표 사진 메타데이터가 없는 Diary는 null 썸네일로 번역한다")
	void preservesMissingThumbnailMetadata() {
		Set<Long> diaryIds = Set.of(10L);
		given(queryDiaryReadUseCase.getDiarySummaries(diaryIds))
				.willReturn(Map.of(
						10L,
						new DiarySummaryView(
								10L,
								"owner-id",
								DiaryVisibility.PUBLIC,
								"content",
								LocalDate.of(2026, 7, 20),
								LocalDateTime.of(2026, 7, 20, 12, 0))));
		given(queryDiaryReadUseCase.getRepresentPhotoPaths(diaryIds)).willReturn(Map.of());

		NotificationDiaryContextView context =
				adapter.loadNotificationDiaryContexts(diaryIds).get(10L);

		assertThat(context.thumbnailUrl()).isNull();
		assertThat(context.diaryDate()).isNull();
	}
}
