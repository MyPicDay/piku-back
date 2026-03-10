package com.pikume.back.diary.application.port.out;

public interface LoadFriendshipForDiaryPort {

	boolean areFriends(String ownerUserId, String viewerUserId);
}
