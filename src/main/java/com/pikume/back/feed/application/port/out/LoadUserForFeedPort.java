package com.pikume.back.feed.application.port.out;

import com.pikume.back.global.dto.RequestMetaInfo;

public interface LoadUserForFeedPort {

	String getUserAvatar(String userId);

	String getUserNickname(String userId);

	String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo);
}
