package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.notification.application.port.out.LoadUserForNotificationPort;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadUserForNotificationPort {

	private final LoadUserPort loadUserPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserNickname(String userId) {
			User user = loadUserPort.findById(userId)
					.orElseThrow(UserNotFoundException::new);
		return user.getNickname();
	}

	@Override
	public String getUserAvatar(String userId) {
			User user = loadUserPort.findById(userId)
					.orElseThrow(UserNotFoundException::new);
		return user.getAvatar();
	}

	@Override
	public String getUserAvatarUrl(String avatar) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar);
	}
}
