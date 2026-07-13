package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.user.application.port.out.QueryProfileSocialMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialAdapterForUser implements QueryProfileSocialMetricsPort {

	private final FriendUseCase friendUseCase;

	@Override
	public int queryFriendCount(String profileUserId) {
		return friendUseCase.countFriends(profileUserId);
	}

	@Override
	public String queryFriendshipStatus(String viewerUserId, String profileUserId) {
		return friendUseCase.getFriendshipStatus(viewerUserId, profileUserId).name();
	}
}
