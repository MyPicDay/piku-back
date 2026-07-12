package com.pikume.back.feed.adapter.out.crosscontext;

import com.pikume.back.feed.application.port.out.LoadUserForFeedPort;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdapterForFeed implements LoadUserForFeedPort {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserAvatar(String userId) {
		return queryUserReferenceUseCase.getUserReference(userId).avatarPath();
	}

	@Override
	public String getUserNickname(String userId) {
		return queryUserReferenceUseCase.getUserReference(userId).nickname();
	}

	@Override
	public String getUserAvatarUrl(String avatar) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar);
	}
}
