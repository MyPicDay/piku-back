package store.piku.back.notification.application.port.out;

import store.piku.back.global.dto.RequestMetaInfo;

public interface LoadUserForNotificationPort {

	String getUserNickname(String userId);

	String getUserAvatar(String userId);

	String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo);
}
