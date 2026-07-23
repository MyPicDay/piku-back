package com.pikume.back.social.application.port.in;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.FriendSummaryResult;

public interface QueryFriendPageUseCase {
	PageResult<FriendSummaryResult> queryFriendPage(PageQuery pageQuery, String userId);

	PageResult<FriendSummaryResult> queryReceivedFriendRequestPage(PageQuery pageQuery, String userId);
}
