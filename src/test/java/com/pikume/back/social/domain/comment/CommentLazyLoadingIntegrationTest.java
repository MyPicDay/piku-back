package com.pikume.back.social.domain.comment;

import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.social.adapter.in.web.dto.CommentListResponseDto;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommentLazyLoadingIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private CommentJpaRepository commentJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("대댓글 DTO 변환에서 parent id 접근은 LAZY 부모를 추가 조회하지 않는다")
	void mappingReplyDtosDoesNotTriggerLazyQueriesForParentId() {
		User owner = saveUser("owner");
		Diary diary = diaryJpaRepository.save(new Diary("comment-diary", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));

		Comment parent1 = commentJpaRepository.save(new Comment("parent-1", owner.getId(), diary.getId()));
		Comment parent2 = commentJpaRepository.save(new Comment("parent-2", owner.getId(), diary.getId()));
		Comment parent3 = commentJpaRepository.save(new Comment("parent-3", owner.getId(), diary.getId()));

		Comment reply1 = saveReply("reply-1", owner.getId(), diary.getId(), parent1);
		Comment reply2 = saveReply("reply-2", owner.getId(), diary.getId(), parent2);
		Comment reply3 = saveReply("reply-3", owner.getId(), diary.getId(), parent3);

		flushAndClear();

		List<Comment> replies = commentJpaRepository.findAllById(List.of(reply1.getId(), reply2.getId(), reply3.getId()));
		Statistics statistics = hibernateStatistics();
		statistics.clear();

		List<CommentListResponseDto> response = replies.stream()
				.map(comment -> CommentListResponseDto.fromEntity(comment, "nickname", null, 0))
				.toList();

		assertThat(response).hasSize(3);
		assertThat(statistics.getPrepareStatementCount())
				.as("현재 코드의 parent.getId() 접근은 Hibernate 프록시 초기화를 일으키지 않아야 한다")
				.isZero();
	}

	private Comment saveReply(String content, String userId, Long diaryId, Comment parent) {
		Comment reply = new Comment(content, userId, diaryId);
		reply.connectParent(parent);
		return commentJpaRepository.save(reply);
	}

	private User saveUser(String suffix) {
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				"avatars/" + suffix + ".png"));
	}
}
