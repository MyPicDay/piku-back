package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedCandidateCollector")
class FeedCandidateCollectorTest {

	@InjectMocks
	private FeedCandidateCollector feedCandidateCollector;

	@Mock
	private LoadDiaryForFeedPort loadDiaryForFeedPort;

	@Mock
	private LoadFeedClickPort loadFeedClickPort;

	@Mock
	private LoadSocialForFeedPort loadSocialForFeedPort;

	@Test
	@DisplayName("친구가 쓴 공개 일기는 추천 보너스용 friend 후보에 포함하고 일반 public 후보에서는 제외한다")
	void collectIncludesFriendAuthoredPublicDiariesInFriendCandidates() {
		List<String> friendIds = List.of("friend-a", "friend-b");

		given(loadFeedClickPort.findClickedDiaryIdsByUserId("viewer")).willReturn(List.of(2L));
		given(loadSocialForFeedPort.getFriendIds("viewer")).willReturn(friendIds);
		given(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(FeedVisibility.FRIENDS, friendIds, 10))
				.willReturn(List.of(1L, 2L));
		given(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(FeedVisibility.PUBLIC, friendIds, 10))
				.willReturn(List.of(3L));
		given(loadDiaryForFeedPort.findFeedIdsByStatus(FeedVisibility.PUBLIC, "viewer", 10))
				.willReturn(List.of(3L, 4L, 5L));

		FeedCandidateCollector.FeedCandidates result = feedCandidateCollector.collect("viewer", 5);

		assertThat(result.orderedDiaryIds()).containsExactly(1L, 3L, 4L, 5L, 2L);
		assertThat(result.friendDiaryIds()).containsExactly(1L, 3L, 2L);
		assertThat(result.publicDiaryIds()).containsExactly(4L, 5L);
	}

	@Test
	@DisplayName("candidate limit 만큼만 우선순위 순서대로 후보를 수집한다")
	void collectCapsCandidatesToRequestedLimit() {
		List<String> friendIds = List.of("friend-a");

		given(loadFeedClickPort.findClickedDiaryIdsByUserId("viewer")).willReturn(List.of());
		given(loadSocialForFeedPort.getFriendIds("viewer")).willReturn(friendIds);
		given(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(FeedVisibility.FRIENDS, friendIds, 6))
				.willReturn(List.of(1L, 2L, 3L));
		given(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(FeedVisibility.PUBLIC, friendIds, 6))
				.willReturn(List.of(4L, 5L));
		given(loadDiaryForFeedPort.findFeedIdsByStatus(FeedVisibility.PUBLIC, "viewer", 6))
				.willReturn(List.of(4L, 5L, 6L, 7L));

		FeedCandidateCollector.FeedCandidates result = feedCandidateCollector.collect("viewer", 3);

		assertThat(result.orderedDiaryIds()).containsExactly(1L, 2L, 3L);
		assertThat(result.friendDiaryIds()).containsExactly(1L, 2L, 3L);
		assertThat(result.publicDiaryIds()).isEmpty();
	}
}
