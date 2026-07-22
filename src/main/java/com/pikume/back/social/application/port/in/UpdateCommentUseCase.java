package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.CommentResult;

public interface UpdateCommentUseCase {
	CommentResult updateComment(Long commentId, String content, String userId);
}
