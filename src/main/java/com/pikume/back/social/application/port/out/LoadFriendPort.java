package com.pikume.back.social.application.port.out;

import java.util.List;

public interface LoadFriendPort {

	boolean existsFriendship(String userId1, String userId2);

	int countByUserId(String userId);

	List<String> findFriendIds(String userId);
}
