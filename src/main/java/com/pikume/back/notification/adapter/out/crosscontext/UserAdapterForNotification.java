package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.notification.application.port.out.LoadUserForNotificationPort;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadUserForNotificationPort {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserNickname(String userId) {
		return queryUserReferenceUseCase.requireUserReference(userId).nickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		return queryUserReferenceUseCase.requireUserReference(userId).avatarPath();
	}

	@Override
	public String getUserAvatarUrl(String avatar) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar);
	}
}
