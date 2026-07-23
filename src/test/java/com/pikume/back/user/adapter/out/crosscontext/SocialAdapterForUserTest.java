package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.social.application.dto.FriendshipStatusResult;
import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SocialAdapterForUserTest {

	@InjectMocks private SocialAdapterForUser adapter;
	@Mock private QueryFriendshipUseCase queryFriendshipUseCase;

	@Test
	void translatesSocialMetricsForUserProfile() {
		given(queryFriendshipUseCase.queryFriendCount("profile")).willReturn(3);
		given(queryFriendshipUseCase.queryFriendshipStatus("viewer", "profile"))
				.willReturn(FriendshipStatusResult.FRIENDS);

		assertThat(adapter.queryFriendCount("profile")).isEqualTo(3);
		assertThat(adapter.queryFriendshipStatus("viewer", "profile")).isEqualTo("FRIENDS");
	}
}
