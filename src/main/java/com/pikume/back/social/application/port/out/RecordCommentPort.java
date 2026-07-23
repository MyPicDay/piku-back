package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.comment.Comment;

public interface RecordCommentPort {
	Comment recordComment(Comment comment);
}
