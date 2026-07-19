package com.pikume.back.feed.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.application.dto.DiaryPhotoView;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryFeedUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;
import com.pikume.back.global.port.out.ResolveImageUrlPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryAdapterForFeed")
class DiaryAdapterForFeedTest {

	@InjectMocks
	private DiaryAdapterForFeed adapter;

	@Mock
	private QueryDiaryFeedUseCase queryDiaryFeedUseCase;
	@Mock
	private QueryDiaryReadUseCase queryDiaryReadUseCase;
	@Mock
	private ResolveImageUrlPort resolveImageUrlPort;

	@Test
	@DisplayName("Diary 공개 모델과 사진 참조를 Feed 목록 원천 모델로 번역한다")
	void translatesDiaryItems() {
		Set<Long> diaryIds = Set.of(1L);
		given(queryDiaryReadUseCase.getDiaryPhotos(diaryIds))
				.willReturn(List.of(new DiaryPhotoView(1L, "photos/one.jpg", true)));
		given(resolveImageUrlPort.getPhotoUrl("photos/one.jpg", true))
				.willReturn("https://cdn.example/one.jpg");
		given(queryDiaryReadUseCase.getDiarySummaries(diaryIds))
				.willReturn(Map.of(
						1L,
						new DiarySummaryView(
								1L,
								"writer-id",
								DiaryVisibility.PUBLIC,
								"content",
								LocalDate.of(2026, 3, 8),
								LocalDateTime.of(2026, 3, 8, 10, 0))));

		FeedDiaryItemSourceView result = adapter.loadDiaryItems(diaryIds).get(1L);

		assertThat(result.status()).isEqualTo(FeedVisibility.PUBLIC);
		assertThat(result.photos()).extracting(photo -> photo.imageUrl())
				.containsExactly("https://cdn.example/one.jpg");
	}
}
