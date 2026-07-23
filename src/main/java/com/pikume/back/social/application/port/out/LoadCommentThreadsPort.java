package com.pikume.back.social.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.domain.comment.Comment;

import java.util.Optional;

public interface LoadCommentThreadsPort {
	Optional<Comment> loadComment(Long commentId);

	PageResult<CommentThreadView> loadRootCommentPage(Long diaryId, PageQuery pageQuery);

	PageResult<CommentThreadView> loadReplyPage(Long parentCommentId, PageQuery pageQuery);
}
