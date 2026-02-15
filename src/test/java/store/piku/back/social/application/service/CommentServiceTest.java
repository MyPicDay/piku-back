package store.piku.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.social.adapter.in.web.dto.CommentDeleteResponseDto;
import store.piku.back.social.adapter.in.web.dto.CommentResponseDto;
import store.piku.back.social.application.port.out.*;
import store.piku.back.social.domain.comment.Comment;
import store.piku.back.social.domain.comment.exception.CommentErrorCode;
import store.piku.back.social.domain.comment.exception.CommentException;
import store.piku.back.social.domain.event.SocialEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	@InjectMocks
	private CommentService commentService;

	@Mock
	private LoadCommentPort loadCommentPort;

	@Mock
	private SaveCommentPort saveCommentPort;

	@Mock
	private LoadDiaryInfoPort loadDiaryInfoPort;

	@Mock
	private LoadUserInfoPort loadUserInfoPort;

	@Mock
	private PublishEventPort publishEventPort;

	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/comments", "TestAgent", "127.0.0.1");

	@Nested
	@DisplayName("createComment - 댓글 생성")
	class CreateComment {

		@Test
		@DisplayName("루트 댓글을 생성하고 알림 이벤트를 발행한다")
		void createRootComment() {
			Comment savedComment = new Comment("댓글 내용", "user-id", 1L);
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(saveCommentPort.save(any(Comment.class))).willReturn(savedComment);

			CommentResponseDto response = commentService.createComment(1L, "댓글 내용", null, "user-id", requestMetaInfo);

			assertThat(response.getContent()).isEqualTo("댓글 내용");
			then(publishEventPort).should().publish(any(SocialEvent.CommentCreatedEvent.class));
		}

		@Test
		@DisplayName("본인 일기에 댓글을 달면 알림 이벤트를 발행하지 않는다")
		void noEventForOwnDiary() {
			Comment savedComment = new Comment("댓글 내용", "owner-id", 1L);
			given(loadUserInfoPort.findUserInfoById("owner-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("owner-id", "주인", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(saveCommentPort.save(any(Comment.class))).willReturn(savedComment);

			commentService.createComment(1L, "댓글 내용", null, "owner-id", requestMetaInfo);

			then(publishEventPort).should(never()).publish(any());
		}

		@Test
		@DisplayName("존재하지 않는 사용자가 댓글을 달면 예외 발생")
		void failsUserNotFound() {
			given(loadUserInfoPort.findUserInfoById("ghost-id")).willReturn(Optional.empty());

			assertThatThrownBy(() -> commentService.createComment(1L, "댓글", null, "ghost-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.INVALID_REQUEST));
		}

		@Test
		@DisplayName("존재하지 않는 일기에 댓글을 달면 예외 발생")
		void failsDiaryNotFound() {
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> commentService.createComment(999L, "댓글", null, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.INVALID_REQUEST));
		}

		@Test
		@DisplayName("존재하지 않는 부모 댓글에 대댓글을 달면 예외 발생")
		void failsParentNotFound() {
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadCommentPort.findById(99L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> commentService.createComment(1L, "대댓글", 99L, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND));
		}

		@Test
		@DisplayName("삭제된 댓글에 대댓글을 달면 예외 발생")
		void failsParentDeleted() {
			Comment deletedParent = new Comment("삭제됨", "user-x", 1L);
			deletedParent.inactive();

			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadCommentPort.findById(10L)).willReturn(Optional.of(deletedParent));

			assertThatThrownBy(() -> commentService.createComment(1L, "대댓글", 10L, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.DELETED_COMMENT));
		}

		@Test
		@DisplayName("대댓글의 대댓글은 허용하지 않는다 (2단 제한)")
		void failsNestedReply() {
			Comment rootComment = new Comment("루트 댓글", "user-a", 1L);
			Comment replyComment = new Comment("대댓글", "user-b", 1L);
			replyComment.connectParent(rootComment);

			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadCommentPort.findById(20L)).willReturn(Optional.of(replyComment));

			assertThatThrownBy(() -> commentService.createComment(1L, "대대댓글", 20L, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.INVALID_PARENT_COMMENT));
		}

		@Test
		@DisplayName("다른 일기의 댓글을 부모로 설정하면 예외 발생")
		void failsParentCommentInDifferentDiary() {
			Comment parentInOtherDiary = new Comment("다른 일기 댓글", "user-a", 2L);

			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadCommentPort.findById(30L)).willReturn(Optional.of(parentInOtherDiary));

			assertThatThrownBy(() -> commentService.createComment(1L, "대댓글", 30L, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.PARENT_COMMENT_NOT_IN_SAME_DIARY));
		}
	}

	@Nested
	@DisplayName("updateComment - 댓글 수정")
	class UpdateComment {

		@Test
		@DisplayName("본인 댓글을 수정한다")
		void updateSuccess() {
			Comment existingComment = new Comment("원래 내용", "user-id", 1L);
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadCommentPort.findById(1L)).willReturn(Optional.of(existingComment));
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(saveCommentPort.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

			CommentResponseDto response = commentService.updateComment(1L, "수정된 내용", "user-id");

			assertThat(response.getContent()).isEqualTo("수정된 내용");
		}

		@Test
		@DisplayName("타인의 댓글을 수정하면 예외 발생")
		void failsUnauthorized() {
			Comment otherUserComment = new Comment("타인 댓글", "other-id", 1L);
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadCommentPort.findById(1L)).willReturn(Optional.of(otherUserComment));

			assertThatThrownBy(() -> commentService.updateComment(1L, "수정", "user-id"))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.UNAUTHORIZED_ACCESS));
		}
	}

	@Nested
	@DisplayName("deleteComment - 댓글 삭제")
	class DeleteComment {

		@Test
		@DisplayName("본인 댓글을 삭제한다")
		void deleteSuccess() {
			Comment existingComment = new Comment("삭제 대상", "user-id", 1L);
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadCommentPort.findById(1L)).willReturn(Optional.of(existingComment));
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);

			CommentDeleteResponseDto response = commentService.deleteComment(1L, "user-id");

			assertThat(response.isSuccess()).isTrue();
			then(saveCommentPort).should().save(any(Comment.class));
		}

		@Test
		@DisplayName("이미 삭제된 댓글을 삭제하면 예외 발생")
		void failsAlreadyDeleted() {
			Comment deletedComment = new Comment("삭제됨", "user-id", 1L);
			deletedComment.inactive();
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadCommentPort.findById(1L)).willReturn(Optional.of(deletedComment));

			assertThatThrownBy(() -> commentService.deleteComment(1L, "user-id"))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.DELETED_COMMENT));
		}
	}

	@Nested
	@DisplayName("countAllCommentsByDiaryId - 전체 댓글 수 조회")
	class CountComments {

		@Test
		@DisplayName("일기의 전체 댓글 수를 조회한다")
		void countSuccess() {
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(loadCommentPort.countAllByDiaryId(1L)).willReturn(15L);

			assertThat(commentService.countAllCommentsByDiaryId(1L)).isEqualTo(15L);
		}

		@Test
		@DisplayName("존재하지 않는 일기의 댓글 수 조회 시 예외 발생")
		void failsDiaryNotFound() {
			given(loadDiaryInfoPort.existsById(999L)).willReturn(false);

			assertThatThrownBy(() -> commentService.countAllCommentsByDiaryId(999L))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.INVALID_REQUEST));
		}
	}
}
