package com.pikume.back.social.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.readmodel.FriendSummaryView;

public interface LoadFriendListViewPort {

	PageResult<FriendSummaryView> loadFriendList(String userId, PageQuery pageQuery);

	PageResult<FriendSummaryView> loadFriendRequests(String toUserId, PageQuery pageQuery);
}
