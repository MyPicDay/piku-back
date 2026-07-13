package com.pikume.back.user.application.port.out;

public interface QueryProfileSocialMetricsPort {

	int queryFriendCount(String profileUserId);

	String queryFriendshipStatus(String viewerUserId, String profileUserId);
}
