package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.CommentResult;

public interface CreateCommentUseCase {
	CommentResult createComment(Long diaryId, String content, Long parentId, String userId);
}
