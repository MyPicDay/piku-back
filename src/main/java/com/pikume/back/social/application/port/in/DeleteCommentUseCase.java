package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.CommentDeleteResult;

public interface DeleteCommentUseCase {
	CommentDeleteResult deleteComment(Long commentId, String userId);
}
