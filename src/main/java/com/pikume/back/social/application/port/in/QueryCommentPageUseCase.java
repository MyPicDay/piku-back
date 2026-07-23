package com.pikume.back.social.application.port.in;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.CommentListItemResult;

public interface QueryCommentPageUseCase {
	PageResult<CommentListItemResult> queryRootCommentPage(Long diaryId, PageQuery pageQuery, String viewerId);

	PageResult<CommentListItemResult> queryReplyPage(Long parentCommentId, PageQuery pageQuery, String viewerId);
}
