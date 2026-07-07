package com.pikume.back.social.application.port.in;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.CommentDeleteResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.application.dto.CommentResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommentUseCase {

	CommentResult createComment(Long diaryId, String content, Long parentId, String userId,
			RequestMetaInfo requestMetaInfo);

	CommentResult updateComment(Long commentId, String content, String userId);

	CommentDeleteResult deleteComment(Long commentId, String userId);

	PageResult<CommentListItemResult> getRootCommentsByDiaryId(Long diaryId, PageQuery pageQuery,
			RequestMetaInfo requestMetaInfo, String viewerId);

	PageResult<CommentListItemResult> getRepliesByParentCommentId(Long parentCommentId, PageQuery pageQuery,
			RequestMetaInfo requestMetaInfo, String viewerId);

	long countActiveCommentsByDiaryId(String viewerId, Long diaryId);

	Map<Long, Long> getCommentCountsForDiaries(List<Long> diaryIds);

	Set<Long> getCommentedDiaryIds(String userId, List<Long> diaryIds);
}
