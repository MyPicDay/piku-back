package store.piku.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.application.port.out.LoadUserForNotificationPort;
import store.piku.back.user._legacy.UserReader;
import store.piku.back.user.domain.User;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadUserForNotificationPort {

	private final UserReader userReader;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserNickname(String userId) {
		User user = userReader.getUserById(userId);
		return user.getNickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		User user = userReader.getUserById(userId);
		return user.getAvatar();
	}

	@Override
	public String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar, requestMetaInfo);
	}
}
