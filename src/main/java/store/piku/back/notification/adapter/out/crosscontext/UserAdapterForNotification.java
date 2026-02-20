package store.piku.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.application.port.out.LoadUserForNotificationPort;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.domain.User;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadUserForNotificationPort {

	private final LoadUserPort loadUserPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserNickname(String userId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return user.getNickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return user.getAvatar();
	}

	@Override
	public String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar, requestMetaInfo);
	}
}
