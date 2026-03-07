package com.pikume.back.social.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.social.application.readmodel.FriendSummaryView;

public interface LoadFriendListViewPort {

	Page<FriendSummaryView> loadFriendList(String userId, Pageable pageable);

	Page<FriendSummaryView> loadFriendRequests(String toUserId, Pageable pageable);
}
