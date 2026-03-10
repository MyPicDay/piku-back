package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.social.application.dto.CommentDeleteResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.application.dto.CommentResult;
import com.pikume.back.social.application.port.out.*;
import com.pikume.back.social.application.readmodel.CommentListView;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.social.domain.comment.exception.CommentErrorCode;
import com.pikume.back.social.domain.comment.exception.CommentException;
import com.pikume.back.social.domain.event.SocialEvent;

import java.time.LocalDateTime;
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
	private LoadCommentListViewPort loadCommentListViewPort;

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
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "user-id")).willReturn(Optional.of("owner-id"));
			given(saveCommentPort.save(any(Comment.class))).willReturn(savedComment);

			CommentResult response = commentService.createComment(1L, "댓글 내용", null, "user-id", requestMetaInfo);

			assertThat(response.content()).isEqualTo("댓글 내용");
			then(publishEventPort).should().publish(any(SocialEvent.CommentCreatedEvent.class));
		}

		@Test
		@DisplayName("본인 일기에 댓글을 달면 알림 이벤트를 발행하지 않는다")
		void noEventForOwnDiary() {
			Comment savedComment = new Comment("댓글 내용", "owner-id", 1L);
			given(loadUserInfoPort.findUserInfoById("owner-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("owner-id", "주인", null)));
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "owner-id")).willReturn(Optional.of("owner-id"));
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
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(999L, "user-id")).willReturn(Optional.empty());

			assertThatThrownBy(() -> commentService.createComment(999L, "댓글", null, "user-id", requestMetaInfo))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.DIARY_NOT_FOUND));
		}

		@Test
		@DisplayName("존재하지 않는 부모 댓글에 대댓글을 달면 예외 발생")
		void failsParentNotFound() {
			given(loadUserInfoPort.findUserInfoById("user-id"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-id", "유저", null)));
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "user-id")).willReturn(Optional.of("owner-id"));
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
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "user-id")).willReturn(Optional.of("owner-id"));
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
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "user-id")).willReturn(Optional.of("owner-id"));
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
			given(loadDiaryInfoPort.findVisibleOwnerUserIdByDiaryId(1L, "user-id")).willReturn(Optional.of("owner-id"));
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
			given(loadDiaryInfoPort.existsVisibleById(1L, "user-id")).willReturn(true);
			given(saveCommentPort.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

			CommentResult response = commentService.updateComment(1L, "수정된 내용", "user-id");

			assertThat(response.content()).isEqualTo("수정된 내용");
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
			given(loadDiaryInfoPort.existsVisibleById(1L, "user-id")).willReturn(true);

			CommentDeleteResult response = commentService.deleteComment(1L, "user-id");

			assertThat(response.success()).isTrue();
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
	@DisplayName("comment list query - 댓글 조회")
	class CommentListQuery {

		@Test
		@DisplayName("루트 댓글 목록은 전용 조회 포트에서 읽고 응답으로 변환한다")
		void loadRootCommentsFromDedicatedQueryPort() {
			PageQuery pageQuery = PageQuery.of(0, 3);
			CommentListView comment = new CommentListView(
					1L,
					10L,
					"user-id",
					"닉네임",
					"avatars/user.png",
					"댓글 내용",
					null,
					2,
					LocalDateTime.of(2026, 3, 8, 10, 0),
					LocalDateTime.of(2026, 3, 8, 10, 30),
					false);
			PageResult<CommentListView> page = new PageResult<>(java.util.List.of(comment), 0, 3, 1);

			given(loadDiaryInfoPort.existsVisibleById(10L, "viewer-id")).willReturn(true);
			given(loadCommentListViewPort.loadRootCommentsByDiaryId(10L, pageQuery)).willReturn(page);
			given(imagePathToUrlConverter.userAvatarImageUrl("avatars/user.png", requestMetaInfo))
					.willReturn("https://localhost:8080/api/avatars/user.png");

			PageResult<CommentListItemResult> response = commentService.getRootCommentsByDiaryId(10L, pageQuery, requestMetaInfo, "viewer-id");

			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().get(0).nickname()).isEqualTo("닉네임");
			assertThat(response.getContent().get(0).replyCount()).isEqualTo(2);
			then(loadCommentListViewPort).should().loadRootCommentsByDiaryId(10L, pageQuery);
			then(loadUserInfoPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("대댓글 목록도 전용 조회 포트에서 읽고 사용자 미존재 시 기본 닉네임을 사용한다")
		void loadRepliesFromDedicatedQueryPort() {
			PageQuery pageQuery = PageQuery.of(0, 2);
			Comment parentComment = new Comment("부모", "owner-id", 11L);
			CommentListView reply = new CommentListView(
					2L,
					11L,
					"reply-user",
					null,
					null,
					"대댓글",
					1L,
					0,
					LocalDateTime.of(2026, 3, 8, 11, 0),
					LocalDateTime.of(2026, 3, 8, 11, 5),
					false);
			PageResult<CommentListView> page = new PageResult<>(java.util.List.of(reply), 0, 2, 1);

			given(loadCommentPort.findById(1L)).willReturn(Optional.of(parentComment));
			given(loadDiaryInfoPort.existsVisibleById(11L, "viewer-id")).willReturn(true);
			given(loadCommentListViewPort.loadRepliesByParentCommentId(1L, pageQuery)).willReturn(page);

			PageResult<CommentListItemResult> response = commentService.getRepliesByParentCommentId(1L, pageQuery, requestMetaInfo, "viewer-id");

			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().get(0).nickname()).isEqualTo("me");
			assertThat(response.getContent().get(0).parentId()).isEqualTo(1L);
			then(loadCommentListViewPort).should().loadRepliesByParentCommentId(1L, pageQuery);
			then(loadUserInfoPort).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("countAllCommentsByDiaryId - 전체 댓글 수 조회")
	class CountComments {

		@Test
		@DisplayName("일기의 전체 댓글 수를 조회한다")
		void countSuccess() {
			given(loadDiaryInfoPort.existsVisibleById(1L, "viewer-id")).willReturn(true);
			given(loadCommentPort.countAllByDiaryId(1L)).willReturn(15L);

			assertThat(commentService.countAllCommentsByDiaryId("viewer-id", 1L)).isEqualTo(15L);
		}

		@Test
		@DisplayName("존재하지 않는 일기의 댓글 수 조회 시 예외 발생")
		void failsDiaryNotFound() {
			given(loadDiaryInfoPort.existsVisibleById(999L, "viewer-id")).willReturn(false);

			assertThatThrownBy(() -> commentService.countAllCommentsByDiaryId("viewer-id", 999L))
					.isInstanceOf(CommentException.class)
					.satisfies(e -> assertThat(((CommentException) e).getErrorCode())
							.isEqualTo(CommentErrorCode.DIARY_NOT_FOUND));
		}
	}
}
