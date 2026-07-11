package com.pikume.back.feed.application.port.out;

public interface LoadUserForFeedPort {

	String getUserAvatar(String userId);

	String getUserNickname(String userId);

	String getUserAvatarUrl(String avatar);
}
