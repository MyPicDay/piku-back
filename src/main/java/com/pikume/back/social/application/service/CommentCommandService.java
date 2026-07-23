package com.pikume.back.social.application.service;

import com.pikume.back.social.application.dto.CommentDeleteResult;
import com.pikume.back.social.application.dto.CommentResult;
import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.policy.AnonymousCommentAccessPolicy;
import com.pikume.back.social.application.port.in.CreateCommentUseCase;
import com.pikume.back.social.application.port.in.DeleteCommentUseCase;
import com.pikume.back.social.application.port.in.UpdateCommentUseCase;
import com.pikume.back.social.application.port.out.LoadCommentThreadsPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordCommentPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.domain.comment.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCommandService implements CreateCommentUseCase, UpdateCommentUseCase, DeleteCommentUseCase {

	private final LoadCommentThreadsPort loadCommentThreadsPort;
	private final RecordCommentPort recordCommentPort;
	private final ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	private final VerifySocialParticipantPort verifySocialParticipantPort;
	private final PublishSocialNotificationEventPort publishSocialNotificationEventPort;
	private final AnonymousCommentAccessPolicy anonymousCommentAccessPolicy;

	@Override
	@Transactional
	public CommentResult createComment(Long diaryId, String content, Long parentId, String userId) {
		verifyParticipant(userId);
		InteractionDiaryView diary = loadVisibleDiary(diaryId, userId);
		Comment comment = new Comment(content, userId, diaryId);
		Comment parent = null;

		if (parentId != null) {
			parent = loadComment(parentId);
			validateActive(parent);
			if (parent.getParent() != null) {
				throw new SocialException(SocialErrorCode.INVALID_PARENT_COMMENT);
			}
			if (!parent.getDiaryId().equals(diaryId)) {
				throw new SocialException(SocialErrorCode.PARENT_COMMENT_NOT_IN_SAME_DIARY);
			}
			if (diary.anonymous() && !anonymousCommentAccessPolicy
					.decide(toThreadView(parent), diary, userId, null)
					.replyAllowed()) {
				throw new SocialException(SocialErrorCode.UNAUTHORIZED_COMMENT_ACCESS);
			}
			comment.connectParent(parent);
		}

		Comment saved = recordCommentPort.recordComment(comment);
		String receiverId = parent != null ? parent.getUserId() : diary.ownerUserId();
		if (!receiverId.equals(userId)) {
			publishSocialNotificationEventPort.publish(new SocialNotificationEvent.CommentCreated(
					receiverId, userId, diaryId, parent != null));
		}
		return new CommentResult(saved.getId(), saved.getContent(), saved.getCreatedAt());
	}

	@Override
	@Transactional
	public CommentResult updateComment(Long commentId, String content, String userId) {
		verifyParticipant(userId);
		Comment comment = loadEditableComment(commentId, userId);
		comment.updateContent(content);
		Comment updated = recordCommentPort.recordComment(comment);
		return new CommentResult(updated.getId(), updated.getContent(), updated.getCreatedAt());
	}

	@Override
	@Transactional
	public CommentDeleteResult deleteComment(Long commentId, String userId) {
		verifyParticipant(userId);
		Comment comment = loadEditableComment(commentId, userId);
		comment.delete();
		recordCommentPort.recordComment(comment);
		return new CommentDeleteResult(true, "성공적으로 댓글을 삭제하였습니다.", commentId);
	}

	private Comment loadEditableComment(Long commentId, String userId) {
		Comment comment = loadComment(commentId);
		validateActive(comment);
		if (!comment.getUserId().equals(userId)) {
			throw new SocialException(SocialErrorCode.UNAUTHORIZED_COMMENT_ACCESS);
		}
		loadVisibleDiary(comment.getDiaryId(), userId);
		return comment;
	}

	private Comment loadComment(Long commentId) {
		return loadCommentThreadsPort.loadComment(commentId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.COMMENT_NOT_FOUND));
	}

	private void validateActive(Comment comment) {
		if (comment.isDeleted()) {
			throw new SocialException(SocialErrorCode.DELETED_COMMENT);
		}
	}

	private InteractionDiaryView loadVisibleDiary(Long diaryId, String viewerId) {
		return resolveInteractionDiaryPort.resolveVisibleDiary(diaryId, viewerId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.DIARY_NOT_FOUND));
	}

	private void verifyParticipant(String userId) {
		if (!verifySocialParticipantPort.participantExists(userId)) {
			throw new SocialException(SocialErrorCode.INVALID_COMMENT_PARTICIPANT);
		}
	}

	private CommentThreadView toThreadView(Comment comment) {
		return new CommentThreadView(
				comment.getId(),
				comment.getDiaryId(),
				comment.getUserId(),
				comment.getContent(),
				comment.getParent() != null ? comment.getParent().getId() : null,
				comment.getChildren().size(),
				comment.getCreatedAt(),
				comment.getUpdatedAt(),
				comment.isDeleted());
	}
}
