package com.pikume.back.social.application.policy;

import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnonymousCommentAccessPolicy")
class AnonymousCommentAccessPolicyTest {

	private final AnonymousCommentAccessPolicy policy = new AnonymousCommentAccessPolicy();

	@Test
	@DisplayName("일기 작성자는 모든 익명 댓글 원문을 볼 수 있다")
	void diaryOwnerCanViewEveryComment() {
		AnonymousCommentAccessDecision decision = policy.decide(
				comment(2L, "reply-author", 1L, false),
				diary("owner", true),
				"owner",
				"root-author");

		assertThat(decision.contentVisible()).isTrue();
	}

	@Test
	@DisplayName("루트 댓글 작성자는 일기 작성자가 남긴 답글 원문을 볼 수 있다")
	void rootAuthorCanViewOwnerReply() {
		AnonymousCommentAccessDecision decision = policy.decide(
				comment(2L, "owner", 1L, false),
				diary("owner", false),
				"root-author",
				"root-author");

		assertThat(decision.contentVisible()).isTrue();
	}

	@Test
	@DisplayName("제3자는 익명 댓글 원문과 행위 권한을 얻지 못한다")
	void thirdPartyCannotViewOrAct() {
		AnonymousCommentAccessDecision decision = policy.decide(
				comment(1L, "root-author", null, false),
				diary("owner", false),
				"third-party",
				null);

		assertThat(decision.contentVisible()).isFalse();
		assertThat(decision.replyAllowed()).isFalse();
		assertThat(decision.editAllowed()).isFalse();
		assertThat(decision.deleteAllowed()).isFalse();
	}

	@Test
	@DisplayName("루트 댓글 작성자는 자신의 활성 루트 댓글에 답글을 작성할 수 있다")
	void rootAuthorCanReplyToOwnActiveRoot() {
		AnonymousCommentAccessDecision decision = policy.decide(
				comment(1L, "root-author", null, false),
				diary("owner", false),
				"root-author",
				null);

		assertThat(decision.replyAllowed()).isTrue();
		assertThat(decision.editAllowed()).isTrue();
		assertThat(decision.deleteAllowed()).isTrue();
	}

	@Test
	@DisplayName("삭제된 댓글에는 답글·수정·삭제 권한을 주지 않는다")
	void deletedCommentHasNoAction() {
		AnonymousCommentAccessDecision decision = policy.decide(
				comment(1L, "root-author", null, true),
				diary("owner", false),
				"root-author",
				null);

		assertThat(decision.contentVisible()).isTrue();
		assertThat(decision.replyAllowed()).isFalse();
		assertThat(decision.editAllowed()).isFalse();
		assertThat(decision.deleteAllowed()).isFalse();
	}

	private CommentThreadView comment(Long id, String userId, Long parentId, boolean deleted) {
		return new CommentThreadView(
				id,
				11L,
				userId,
				"내용",
				parentId,
				0,
				LocalDateTime.of(2026, 7, 22, 10, 0),
				LocalDateTime.of(2026, 7, 22, 10, 0),
				deleted);
	}

	private InteractionDiaryView diary(String ownerId, boolean viewerOwner) {
		return new InteractionDiaryView(11L, ownerId, true, viewerOwner);
	}
}
