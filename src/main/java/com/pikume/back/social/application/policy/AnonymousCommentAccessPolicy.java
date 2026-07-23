package com.pikume.back.social.application.policy;

import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class AnonymousCommentAccessPolicy {

	public AnonymousCommentAccessDecision decide(
			CommentThreadView comment,
			InteractionDiaryView diary,
			String viewerUserId,
			String parentAuthorUserId) {
		boolean ownComment = Objects.equals(comment.authorUserId(), viewerUserId);
		boolean ownerReplyToViewer = Objects.equals(comment.authorUserId(), diary.ownerUserId())
				&& Objects.equals(parentAuthorUserId, viewerUserId);
		boolean contentVisible = !diary.anonymous()
				|| diary.viewerOwner()
				|| ownComment
				|| ownerReplyToViewer;
		boolean active = !comment.deleted();
		boolean replyAllowed = active
				&& comment.parentCommentId() == null
				&& contentVisible
				&& (diary.viewerOwner() || ownComment);
		boolean manageAllowed = active && ownComment;

		return new AnonymousCommentAccessDecision(
				contentVisible,
				replyAllowed,
				manageAllowed,
				manageAllowed);
	}
}
