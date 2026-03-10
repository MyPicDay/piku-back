package com.pikume.back.social.application.port.out;

import java.util.List;
import java.util.Set;

public interface LoadFriendPort {

	boolean existsFriendship(String userId1, String userId2);

	int countByUserId(String userId);

	List<String> findFriendIds(String userId);

	List<String> findFriendIdsWithinTargets(String userId, Set<String> targetUserIds);
}
