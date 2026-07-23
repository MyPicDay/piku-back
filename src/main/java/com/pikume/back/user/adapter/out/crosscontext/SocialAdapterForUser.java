package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;
import com.pikume.back.user.application.port.out.QueryProfileSocialMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialAdapterForUser implements QueryProfileSocialMetricsPort {

	private final QueryFriendshipUseCase queryFriendshipUseCase;

	@Override
	public int queryFriendCount(String profileUserId) {
		return queryFriendshipUseCase.queryFriendCount(profileUserId);
	}

	@Override
	public String queryFriendshipStatus(String viewerUserId, String profileUserId) {
		return queryFriendshipUseCase.queryFriendshipStatus(viewerUserId, profileUserId).name();
	}
}
