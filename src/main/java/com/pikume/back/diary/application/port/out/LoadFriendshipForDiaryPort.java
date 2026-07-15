package com.pikume.back.diary.application.port.out;

import java.util.List;

public interface LoadFriendshipForDiaryPort {

	boolean areFriends(String ownerUserId, String viewerUserId);

	List<String> findFriendIds(String userId);
}
