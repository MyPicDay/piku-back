package com.pikume.back.social.application.service;

import com.pikume.back.social.application.port.out.LoadDiaryLikesPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LikeQueryServiceTest {

	@InjectMocks private LikeQueryService service;
	@Mock private LoadDiaryLikesPort loadDiaryLikesPort;
	@Mock private ResolveInteractionDiaryPort resolveInteractionDiaryPort;

	@Test
	void convertsTypedCountsToMap() {
		given(loadDiaryLikesPort.loadActiveLikeCounts(List.of(1L, 2L)))
				.willReturn(List.of(new DiaryEngagementCount(1L, 4L)));

		assertThat(service.queryLikeCounts(List.of(1L, 2L))).containsEntry(1L, 4L);
	}

	@Test
	void anonymousViewerHasNoLikedDiaries() {
		assertThat(service.queryLikedDiaryIds(null, List.of(1L))).isEmpty();
	}
}
