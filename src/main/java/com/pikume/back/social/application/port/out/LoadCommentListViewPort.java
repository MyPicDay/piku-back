package com.pikume.back.social.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.readmodel.CommentListView;

public interface LoadCommentListViewPort {

	PageResult<CommentListView> loadRootCommentsByDiaryId(Long diaryId, PageQuery pageQuery);

	PageResult<CommentListView> loadRepliesByParentCommentId(Long parentCommentId, PageQuery pageQuery);
}
