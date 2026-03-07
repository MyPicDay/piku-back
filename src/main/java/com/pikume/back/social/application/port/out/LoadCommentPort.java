package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.comment.Comment;

import java.util.Optional;

public interface LoadCommentPort {

	Optional<Comment> findById(Long commentId);

	long countAllByDiaryId(Long diaryId);
}
