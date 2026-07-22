package com.pikume.back.feed.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;
import com.pikume.back.social.application.port.in.QueryCommentEngagementUseCase;
import com.pikume.back.social.application.port.in.QueryDiaryLikeEngagementUseCase;
import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAdapterForFeed")
class SocialAdapterForFeedTest {

	@InjectMocks
	private SocialAdapterForFeed adapter;

	@Mock
	private QueryFriendshipUseCase queryFriendshipUseCase;
	@Mock
	private QueryDiaryLikeEngagementUseCase queryDiaryLikeEngagementUseCase;
	@Mock
	private QueryCommentEngagementUseCase queryCommentEngagementUseCase;

	@Test
	@DisplayName("Social 일괄 반응 결과를 Diary별 Feed 반응 모델로 번역한다")
	void translatesEngagements() {
		List<Long> diaryIds = List.of(1L, 2L);
		given(queryCommentEngagementUseCase.queryCommentCounts(diaryIds))
				.willReturn(Map.of(1L, 3L));
		given(queryDiaryLikeEngagementUseCase.queryLikeCounts(diaryIds))
				.willReturn(Map.of(1L, 5L));
		given(queryDiaryLikeEngagementUseCase.queryLikedDiaryIds("viewer-id", diaryIds))
				.willReturn(Set.of(2L));

		Map<Long, FeedEngagementView> result = adapter.loadEngagements("viewer-id", diaryIds);

		assertThat(result.get(1L).commentCount()).isEqualTo(3L);
		assertThat(result.get(1L).likeCount()).isEqualTo(5L);
		assertThat(result.get(1L).liked()).isFalse();
		assertThat(result.get(2L).commentCount()).isZero();
		assertThat(result.get(2L).likeCount()).isZero();
		assertThat(result.get(2L).liked()).isTrue();
	}
}
