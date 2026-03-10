package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.comment.Comment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoadCommentPort {

	Optional<Comment> findById(Long commentId);

	long countAllByDiaryId(Long diaryId);

	List<Object[]> countAllByDiaryIds(Collection<Long> diaryIds);

	Set<Long> findCommentedDiaryIdsByUserId(String userId, Collection<Long> diaryIds);
}
