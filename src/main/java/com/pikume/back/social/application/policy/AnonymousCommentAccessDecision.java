package com.pikume.back.social.application.policy;

public record AnonymousCommentAccessDecision(
		boolean contentVisible,
		boolean replyAllowed,
		boolean editAllowed,
		boolean deleteAllowed) {
}
