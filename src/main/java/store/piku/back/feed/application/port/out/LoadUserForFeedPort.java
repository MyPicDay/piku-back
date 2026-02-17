package store.piku.back.feed.application.port.out;

import store.piku.back.global.dto.RequestMetaInfo;

public interface LoadUserForFeedPort {

	String getUserAvatar(String userId);

	String getUserNickname(String userId);

	String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo);
}
