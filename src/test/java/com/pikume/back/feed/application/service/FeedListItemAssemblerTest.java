package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.policy.FeedAuthorMaskingPolicy;
import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryItemSourcesPort;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadFeedItemEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.application.readmodel.FeedPhotoReferenceView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedListItemAssembler")
class FeedListItemAssemblerTest {

	@Mock
	private LoadFeedDiaryItemSourcesPort loadFeedDiaryItemSourcesPort;
	@Mock
	private LoadFeedAuthorsPort loadFeedAuthorsPort;
	@Mock
	private LoadFeedItemEngagementPort loadFeedItemEngagementPort;
	@Mock
	private LoadFeedFriendshipPort loadFeedFriendshipPort;

	private FeedListItemAssembler assembler;

	@BeforeEach
	void setUp() {
		assembler = new FeedListItemAssembler(
				loadFeedDiaryItemSourcesPort,
				loadFeedAuthorsPort,
				loadFeedItemEngagementPort,
				loadFeedFriendshipPort,
				new FeedAuthorMaskingPolicy());
	}

	@Nested
	@DisplayName("assemble")
	class Assemble {

		@Test
		@DisplayName("후보 ID 순서를 보존하고 Provider 결과를 Feed 결과로 조합한다")
		void preservesCandidateOrder() {
			FeedDiaryItemSourceView first = diary(1L, "writer-1", FeedVisibility.PUBLIC);
			FeedDiaryItemSourceView second = diary(2L, "writer-2", FeedVisibility.PUBLIC);
			given(loadFeedDiaryItemSourcesPort.loadDiaryItemSources(Set.of(2L, 1L)))
					.willReturn(Map.of(1L, first, 2L, second));
			given(loadFeedAuthorsPort.loadAuthors(Set.of("writer-1", "writer-2")))
					.willReturn(Map.of(
							"writer-1", new FeedAuthorView("writer-1", "first", "first-avatar"),
							"writer-2", new FeedAuthorView("writer-2", "second", "second-avatar")));
			given(loadFeedFriendshipPort.loadFriendStatuses("viewer", Set.of("writer-1", "writer-2")))
					.willReturn(Map.of("writer-1", FeedFriendStatus.FRIENDS));
			given(loadFeedItemEngagementPort.loadEngagements("viewer", List.of(2L, 1L)))
					.willReturn(Map.of(
							1L, new FeedEngagementView(1L, 3L, 5L, true),
							2L, new FeedEngagementView(2L, 1L, 2L, false)));

			List<FeedListItemView> result = assembler.assemble(List.of(2L, 1L), "viewer");

			assertThat(result).extracting(FeedListItemView::diaryId).containsExactly(2L, 1L);
			assertThat(result.get(0).nickname()).isEqualTo("second");
			assertThat(result.get(0).commentCount()).isEqualTo(1L);
			assertThat(result.get(0).friendStatus()).isEqualTo(FeedFriendStatus.NONE);
			assertThat(result.get(1).imageUrls()).containsExactly("image-1");
			assertThat(result.get(1).likeCount()).isEqualTo(5L);
			assertThat(result.get(1).liked()).isTrue();
		}

		@Test
		@DisplayName("누락된 일기는 제외하고 익명 작성자와 누락 반응은 현재 기본값으로 조합한다")
		void handlesMissingSourceAndAnonymousAuthor() {
			FeedDiaryItemSourceView anonymous = diary(2L, "writer-2", FeedVisibility.ANONYMOUS);
			given(loadFeedDiaryItemSourcesPort.loadDiaryItemSources(Set.of(1L, 2L)))
					.willReturn(Map.of(2L, anonymous));
			given(loadFeedAuthorsPort.loadAuthors(Set.of())).willReturn(Map.of());
			given(loadFeedFriendshipPort.loadFriendStatuses("writer-2", Set.of())).willReturn(Map.of());
			given(loadFeedItemEngagementPort.loadEngagements("writer-2", List.of(1L, 2L))).willReturn(Map.of());

			List<FeedListItemView> result = assembler.assemble(List.of(1L, 2L), "writer-2");

			assertThat(result).hasSize(1);
			assertThat(result.get(0).nickname()).isEqualTo("익명");
			assertThat(result.get(0).userId()).isNull();
			assertThat(result.get(0).avatarUrl()).isNull();
			assertThat(result.get(0).friendStatus()).isEqualTo(FeedFriendStatus.ANONYMOUS);
			assertThat(result.get(0).commentCount()).isZero();
			assertThat(result.get(0).likeCount()).isZero();
			assertThat(result.get(0).liked()).isFalse();
			assertThat(result.get(0).owner()).isTrue();
			then(loadFeedAuthorsPort).should().loadAuthors(Set.of());
		}
	}

	private FeedDiaryItemSourceView diary(Long diaryId, String userId, FeedVisibility visibility) {
		return new FeedDiaryItemSourceView(
				diaryId,
				userId,
				visibility,
				"content-" + diaryId,
				List.of(new FeedPhotoReferenceView("image-" + diaryId)),
				LocalDate.of(2026, 3, diaryId.intValue()),
				LocalDateTime.of(2026, 3, diaryId.intValue(), 10, 0));
	}
}
