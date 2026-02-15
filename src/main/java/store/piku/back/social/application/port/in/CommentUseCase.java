package store.piku.back.social.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.social.adapter.in.web.dto.CommentListResponseDto;
import store.piku.back.social.adapter.in.web.dto.CommentResponseDto;
import store.piku.back.social.adapter.in.web.dto.CommentDeleteResponseDto;

public interface CommentUseCase {

	CommentResponseDto createComment(Long diaryId, String content, Long parentId, String userId,
			RequestMetaInfo requestMetaInfo);

	CommentResponseDto updateComment(Long commentId, String content, String userId);

	CommentDeleteResponseDto deleteComment(Long commentId, String userId);

	Page<CommentListResponseDto> getRootCommentsByDiaryId(Long diaryId, Pageable pageable,
			RequestMetaInfo requestMetaInfo);

	Page<CommentListResponseDto> getRepliesByParentCommentId(Long parentCommentId, Pageable pageable,
			RequestMetaInfo requestMetaInfo);

	long countAllCommentsByDiaryId(Long diaryId);
}
