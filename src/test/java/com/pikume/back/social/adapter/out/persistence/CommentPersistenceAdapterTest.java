package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.social.domain.comment.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentPersistenceAdapter")
class CommentPersistenceAdapterTest {

	@InjectMocks
	private CommentPersistenceAdapter adapter;

	@Mock
	private CommentJpaRepository commentJpaRepository;

	@Test
	@DisplayName("댓글을 기록하고 저장된 댓글을 반환한다")
	void recordsComment() {
		Comment comment = new Comment("content", "user", 1L);
		given(commentJpaRepository.save(comment)).willReturn(comment);

		Comment result = adapter.recordComment(comment);

		assertThat(result).isSameAs(comment);
		then(commentJpaRepository).should().save(comment);
	}

	@Test
	@DisplayName("예상하지 못한 저장소 장애는 Social 비즈니스 오류로 바꾸지 않고 전파한다")
	void propagatesUnexpectedStorageFailure() {
		Comment comment = new Comment("content", "user", 1L);
		DataAccessResourceFailureException failure =
				new DataAccessResourceFailureException("storage unavailable");
		given(commentJpaRepository.save(comment)).willThrow(failure);

		assertThatThrownBy(() -> adapter.recordComment(comment))
				.isSameAs(failure);
	}
}
