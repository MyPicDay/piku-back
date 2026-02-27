package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.comment.Comment;

public interface SaveCommentPort {

	Comment save(Comment comment);
}
