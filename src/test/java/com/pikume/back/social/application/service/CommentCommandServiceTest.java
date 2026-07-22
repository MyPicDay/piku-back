package com.pikume.back.social.application.service;

import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.policy.AnonymousCommentAccessPolicy;
import com.pikume.back.social.application.port.out.LoadCommentThreadsPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordCommentPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.domain.comment.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

	@Mock private LoadCommentThreadsPort loadCommentThreadsPort;
	@Mock private RecordCommentPort recordCommentPort;
	@Mock private ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	@Mock private VerifySocialParticipantPort verifySocialParticipantPort;
	@Mock private PublishSocialNotificationEventPort eventPort;
	private CommentCommandService service;

	@BeforeEach
	void setUp() {
		service = new CommentCommandService(
				loadCommentThreadsPort,
				recordCommentPort,
				resolveInteractionDiaryPort,
				verifySocialParticipantPort,
				eventPort,
				new AnonymousCommentAccessPolicy());
	}

	@Test
	void createsRootCommentAndPublishesNotification() {
		given(verifySocialParticipantPort.participantExists("author")).willReturn(true);
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "author"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", false, false)));
		given(recordCommentPort.recordComment(any())).willAnswer(invocation -> invocation.getArgument(0));

		service.createComment(1L, "내용", null, "author");

		verify(eventPort).publish(new SocialNotificationEvent.CommentCreated("owner", "author", 1L, false));
	}

	@Test
	void rejectsAnonymousReplyFromThirdParty() {
		Comment root = new Comment("루트", "root-author", 1L);
		given(verifySocialParticipantPort.participantExists("third")).willReturn(true);
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "third"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", true, false)));
		given(loadCommentThreadsPort.loadComment(10L)).willReturn(Optional.of(root));

		assertThatThrownBy(() -> service.createComment(1L, "답글", 10L, "third"))
				.isInstanceOfSatisfying(SocialException.class,
						exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
								.isEqualTo(SocialErrorCode.UNAUTHORIZED_COMMENT_ACCESS));
	}

	@Test
	void preventsAnotherParticipantFromUpdatingComment() {
		Comment comment = new Comment("내용", "author", 1L);
		given(verifySocialParticipantPort.participantExists("other")).willReturn(true);
		given(loadCommentThreadsPort.loadComment(10L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() -> service.updateComment(10L, "수정", "other"))
				.isInstanceOfSatisfying(SocialException.class,
						exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
								.isEqualTo(SocialErrorCode.UNAUTHORIZED_COMMENT_ACCESS));
	}
}
