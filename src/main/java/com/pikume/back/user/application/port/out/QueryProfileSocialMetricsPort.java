package com.pikume.back.user.application.port.out;

public interface QueryProfileSocialMetricsPort {

	int countFriends(String profileUserId);

	String getFriendshipStatus(String viewerUserId, String profileUserId);
}
