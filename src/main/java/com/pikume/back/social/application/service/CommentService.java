package com.pikume.back.social.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.social.application.dto.CommentDeleteResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.application.dto.CommentResult;
import com.pikume.back.social.application.port.in.CommentUseCase;
import com.pikume.back.social.application.port.out.*;
import com.pikume.back.social.application.readmodel.CommentListView;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.social.domain.comment.exception.CommentErrorCode;
import com.pikume.back.social.domain.comment.exception.CommentException;
import com.pikume.back.social.domain.event.SocialEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService implements CommentUseCase {

	private final LoadCommentPort loadCommentPort;
	private final SaveCommentPort saveCommentPort;
	private final LoadDiaryInfoPort loadDiaryInfoPort;
	private final LoadUserInfoPort loadUserInfoPort;
	private final LoadCommentListViewPort loadCommentListViewPort;
	private final PublishEventPort publishEventPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	@Transactional
	public CommentResult createComment(Long diaryId, String content, Long parentId, String userId,
			RequestMetaInfo requestMetaInfo) {
		// 사용자/일기 존재 확인
		loadUserInfoPort.findUserInfoById(userId)
				.orElseThrow(() -> new CommentException(CommentErrorCode.INVALID_REQUEST));

		String diaryOwnerId = loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(diaryId, userId)
				.orElseThrow(() -> new CommentException(CommentErrorCode.DIARY_NOT_FOUND));

		Comment comment = new Comment(content, userId, diaryId);

		if (parentId != null) {
			Comment parentComment = validateCommentExists(parentId);
			validateCommentNotDeleted(parentComment);

			if (parentComment.getParent() != null) {
				throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT);
			}

			if (!parentComment.getDiaryId().equals(diaryId)) {
				throw new CommentException(CommentErrorCode.PARENT_COMMENT_NOT_IN_SAME_DIARY);
			}
			comment.connectParent(parentComment);
		}

		Comment savedComment = saveCommentToDb(comment, userId, diaryId);
		log.info("사용자 {}님이 {} 일기에 댓글 등록 완료", userId, diaryId);

		// 알림 이벤트 발행
		String receiverId = diaryOwnerId;
		boolean isReply = false;

		if (parentId != null) {
			Comment parentComment = validateCommentExists(parentId);
			receiverId = parentComment.getUserId();
			isReply = true;
		}

		if (!receiverId.equals(userId)) {
			publishEventPort.publish(new SocialEvent.CommentCreatedEvent(
					receiverId, userId, diaryId, isReply));
		}

		return new CommentResult(
				savedComment.getId(),
				savedComment.getContent(),
				savedComment.getCreatedAt());
	}

	@Override
	@Transactional
	public CommentResult updateComment(Long commentId, String content, String userId) {
		loadUserInfoPort.findUserInfoById(userId)
				.orElseThrow(() -> new CommentException(CommentErrorCode.INVALID_REQUEST));
		Comment comment = validateCommentForEditOrDelete(commentId, userId);
		comment.updateContent(content);
		Comment updatedComment = saveCommentToDb(comment, userId, comment.getDiaryId());

		log.info("사용자 {}님이 댓글 {} 수정 완료", userId, updatedComment.getId());

		return new CommentResult(
				updatedComment.getId(),
				updatedComment.getContent(),
				updatedComment.getCreatedAt());
	}

	@Override
	@Transactional
	public CommentDeleteResult deleteComment(Long commentId, String userId) {
		loadUserInfoPort.findUserInfoById(userId)
				.orElseThrow(() -> new CommentException(CommentErrorCode.INVALID_REQUEST));
		Comment comment = validateCommentForEditOrDelete(commentId, userId);
		comment.inactive();
		saveCommentPort.save(comment);
		log.info("사용자 {}님이 댓글 {} 삭제 완료", userId, commentId);

		return new CommentDeleteResult(true, "성공적으로 댓글을 삭제하였습니다.", commentId);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<CommentListItemResult> getRootCommentsByDiaryId(Long diaryId, PageQuery pageQuery,
			RequestMetaInfo requestMetaInfo, String viewerId) {
		if (!loadDiaryInfoPort.existsVisibleById(diaryId, viewerId)) {
			throw new CommentException(CommentErrorCode.DIARY_NOT_FOUND);
		}
		PageResult<CommentListView> rootCommentsPage = loadCommentListViewPort.loadRootCommentsByDiaryId(diaryId, pageQuery);
		log.info("일기 ID {}에 대한 루트 댓글 {}개 조회 완료.", diaryId, rootCommentsPage.getTotalElements());

		return rootCommentsPage.map(comment -> toCommentListResponse(comment, requestMetaInfo));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<CommentListItemResult> getRepliesByParentCommentId(Long parentCommentId, PageQuery pageQuery,
			RequestMetaInfo requestMetaInfo, String viewerId) {
		Comment parentComment = validateCommentExists(parentCommentId);
		if (!loadDiaryInfoPort.existsVisibleById(parentComment.getDiaryId(), viewerId)) {
			throw new CommentException(CommentErrorCode.DIARY_NOT_FOUND);
		}
		PageResult<CommentListView> repliesPage = loadCommentListViewPort.loadRepliesByParentCommentId(parentCommentId, pageQuery);
		log.info("부모 댓글 ID {}에 대한 대댓글 {}개 조회 완료.", parentCommentId, repliesPage.getTotalElements());

		return repliesPage.map(comment -> toCommentListResponse(comment, requestMetaInfo));
	}

	@Override
	@Transactional(readOnly = true)
	public long countAllCommentsByDiaryId(String viewerId, Long diaryId) {
		if (!loadDiaryInfoPort.existsVisibleById(diaryId, viewerId)) {
			throw new CommentException(CommentErrorCode.DIARY_NOT_FOUND);
		}
		long count = loadCommentPort.countAllByDiaryId(diaryId);
		log.info("일기 ID {}에 달린 전체 댓글 수: {}", diaryId, count);
		return count;
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Map<Long, Long> getCommentCountsForDiaries(java.util.List<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return java.util.Map.of();
		}

		return loadCommentPort.countAllByDiaryIds(diaryIds).stream()
				.collect(java.util.stream.Collectors.toMap(
						row -> (Long) row[0],
						row -> ((Number) row[1]).longValue()));
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Set<Long> getCommentedDiaryIds(String userId, java.util.List<Long> diaryIds) {
		if (userId == null || userId.isBlank() || diaryIds == null || diaryIds.isEmpty()) {
			return java.util.Set.of();
		}

		return loadCommentPort.findCommentedDiaryIdsByUserId(userId, diaryIds);
	}

	private Comment saveCommentToDb(Comment comment, String userId, Long diaryId) {
		try {
			return saveCommentPort.save(comment);
		} catch (DataAccessException e) {
			log.error("DB 댓글 저장/수정 실패. user: {}, diaryId: {}, commentId: {}", userId, diaryId, comment.getId(), e);
			throw new CommentException(CommentErrorCode.DATABASE_ERROR, e);
		}
	}

	private Comment validateCommentExists(Long commentId) {
		return loadCommentPort.findById(commentId)
				.orElseThrow(() -> {
					log.error("댓글 ID {}을(를) 찾을 수 없습니다.", commentId);
					return new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);
				});
	}

	private void validateCommentNotDeleted(Comment comment) {
		if (comment.isDeleted()) {
			log.warn("이미 삭제된 댓글입니다. commentId={}", comment.getId());
			throw new CommentException(CommentErrorCode.DELETED_COMMENT);
		}
	}

	private CommentListItemResult toCommentListResponse(CommentListView comment, RequestMetaInfo requestMetaInfo) {
		if (comment.deleted()) {
			return new CommentListItemResult(
					comment.commentId(),
					comment.diaryId(),
					null,
					null,
					null,
					"삭제된 댓글입니다.",
					comment.parentId(),
					comment.createdAt(),
					comment.updatedAt(),
					comment.replyCount());
		}

		String nickname = comment.nickname() != null ? comment.nickname() : "me";
		String avatarUrl = comment.avatarPath() != null
				? imagePathToUrlConverter.userAvatarImageUrl(comment.avatarPath(), requestMetaInfo)
				: null;

		return new CommentListItemResult(
				comment.commentId(),
				comment.diaryId(),
				comment.userId(),
				nickname,
				avatarUrl,
				comment.content(),
				comment.parentId(),
				comment.createdAt(),
				comment.updatedAt(),
				comment.replyCount());
	}

	private Comment validateCommentForEditOrDelete(Long commentId, String userId) {
		Comment comment = validateCommentExists(commentId);
		validateCommentNotDeleted(comment);

		if (!comment.getUserId().equals(userId)) {
			log.error("본인의 댓글만 수정/삭제할 수 있습니다. commentId={}, userId={}", commentId, userId);
			throw new CommentException(CommentErrorCode.UNAUTHORIZED_ACCESS);
		}

		if (!loadDiaryInfoPort.existsVisibleById(comment.getDiaryId(), userId)) {
			log.error("댓글 {}이 연결된 다이어리를 찾을 수 없습니다.", commentId);
			throw new CommentException(CommentErrorCode.DIARY_NOT_FOUND);
		}

		return comment;
	}
}
