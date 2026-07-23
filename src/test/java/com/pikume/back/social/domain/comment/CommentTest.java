package com.pikume.back.social.domain.comment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Comment")
class CommentTest {

	@Test
	@DisplayName("같은 일기의 루트 댓글에 답글을 연결한다")
	void connectsReplyToRootInSameDiary() {
		Comment root = new Comment("루트", "root-author", 1L);
		Comment reply = new Comment("답글", "reply-author", 1L);

		reply.connectParent(root);

		assertThat(reply.getParent()).isSameAs(root);
		assertThat(root.getChildren()).containsExactly(reply);
	}

	@Test
	@DisplayName("다른 일기의 댓글은 부모로 연결할 수 없다")
	void rejectsParentFromDifferentDiary() {
		Comment root = new Comment("루트", "root-author", 2L);
		Comment reply = new Comment("답글", "reply-author", 1L);

		assertThatThrownBy(() -> reply.connectParent(root))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("답글에는 다시 답글을 연결할 수 없다")
	void rejectsReplyAsParent() {
		Comment root = new Comment("루트", "root-author", 1L);
		Comment reply = new Comment("답글", "reply-author", 1L);
		Comment nestedReply = new Comment("대대댓글", "nested-author", 1L);
		reply.connectParent(root);

		assertThatThrownBy(() -> nestedReply.connectParent(reply))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("삭제 상태를 자신의 생명주기로 관리한다")
	void ownsDeletionState() {
		Comment comment = new Comment("내용", "author", 1L);

		comment.delete();

		assertThat(comment.isDeleted()).isTrue();
	}
}
