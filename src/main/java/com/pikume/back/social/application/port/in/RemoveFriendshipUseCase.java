package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.FriendRemovalResult;

public interface RemoveFriendshipUseCase {
	FriendRemovalResult removeFriend(String currentUserId, String targetUserId);
}
