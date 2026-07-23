package com.pikume.back.feed.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdapterForFeed")
class UserAdapterForFeedTest {

	@InjectMocks
	private UserAdapterForFeed adapter;

	@Mock
	private QueryUserSummaryUseCase queryUserSummaryUseCase;
	@Mock
	private ResolveObjectUrlPort resolveObjectUrlPort;

	@Test
	@DisplayName("User 요약과 아바타 저장 참조를 Feed 작성자 모델로 번역한다")
	void translatesAuthors() {
		Set<String> userIds = Set.of("writer-id");
		given(queryUserSummaryUseCase.queryUserSummaries(userIds))
				.willReturn(Map.of(
						"writer-id",
						new UserSummaryView("writer-id", "writer", "avatars/writer.png")));
		given(resolveObjectUrlPort.resolveObjectUrl("avatars/writer.png", false))
				.willReturn("https://cdn.example/avatar.png");

		FeedAuthorView result = adapter.loadAuthors(userIds).get("writer-id");

		assertThat(result.nickname()).isEqualTo("writer");
		assertThat(result.avatarUrl()).isEqualTo("https://cdn.example/avatar.png");
	}
}
