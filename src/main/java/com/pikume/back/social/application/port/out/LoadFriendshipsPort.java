package com.pikume.back.social.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.readmodel.FriendReferenceView;

import java.util.List;
import java.util.Set;

public interface LoadFriendshipsPort {
	boolean friendshipExists(String userId1, String userId2);

	int countFriendships(String userId);

	List<String> loadFriendIds(String userId);

	List<String> loadFriendIdsWithin(String userId, Set<String> targetUserIds);

	PageResult<FriendReferenceView> loadFriendPage(String userId, PageQuery pageQuery);
}
