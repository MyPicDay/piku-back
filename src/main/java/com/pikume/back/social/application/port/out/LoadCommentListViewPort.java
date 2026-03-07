package com.pikume.back.social.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.social.application.readmodel.CommentListView;

public interface LoadCommentListViewPort {

	Page<CommentListView> loadRootCommentsByDiaryId(Long diaryId, Pageable pageable);

	Page<CommentListView> loadRepliesByParentCommentId(Long parentCommentId, Pageable pageable);
}
