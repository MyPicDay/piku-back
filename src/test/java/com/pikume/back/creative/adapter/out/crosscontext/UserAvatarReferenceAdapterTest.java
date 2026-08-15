package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarReferenceAdapter")
class UserAvatarReferenceAdapterTest {

	@Mock
	private QueryUserSummaryUseCase queryUserSummaryUseCase;

	@Test
	@DisplayName("User 공개 조회 결과를 Creative의 아바타 참조로 번역한다")
	void translatesUserSummaryToAvatarReference() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of(
						"user-1",
						new UserSummaryView(
								"user-1", "피쿠",
								new UserAvatarReference(
										"public/characters/fixed/base.webp", false, true))));
		UserAvatarReferenceAdapter adapter = new UserAvatarReferenceAdapter(queryUserSummaryUseCase);

		assertThat(adapter.loadUserAvatarReference("user-1"))
				.contains("public/characters/fixed/base.webp");
	}

	@Test
	@DisplayName("사용자 또는 아바타 참조가 없으면 빈 결과를 반환한다")
	void returnsEmptyWhenAvatarReferenceIsMissing() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1"))).willReturn(Map.of());
		UserAvatarReferenceAdapter adapter = new UserAvatarReferenceAdapter(queryUserSummaryUseCase);

		assertThat(adapter.loadUserAvatarReference("user-1")).isEmpty();
	}
}
