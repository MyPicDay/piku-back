package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserAdapterForSocialTest {

	@InjectMocks private UserAdapterForSocial adapter;
	@Mock private QueryUserReferenceUseCase queryUserReferenceUseCase;
	@Mock private QueryUserSummaryUseCase queryUserSummaryUseCase;
	@Mock private ResolveObjectUrlPort resolveObjectUrlPort;

	@Test
	void translatesPublicUserContractsToSocialProfile() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user")))
				.willReturn(Map.of(
						"user",
						new UserSummaryView(
								"user", "닉네임",
								new UserAvatarReference("avatars/user.png", false, false))));
		given(resolveObjectUrlPort.resolveObjectUrl("avatars/user.png", false))
				.willReturn("https://cdn/avatar.png");

		var result = adapter.loadProfiles(Set.of("user")).get("user");

		assertThat(result.nickname()).isEqualTo("닉네임");
		assertThat(result.avatarUrl()).isEqualTo("https://cdn/avatar.png");
	}

	@Test
	void verifiesParticipantThroughReferenceContract() {
		given(queryUserReferenceUseCase.queryUserReference("user"))
				.willReturn(Optional.of(new UserReferenceView("user", "닉네임", null)));

		assertThat(adapter.participantExists("user")).isTrue();
	}
}
