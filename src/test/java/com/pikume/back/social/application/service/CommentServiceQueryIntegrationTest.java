package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CommentServiceQueryIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private CommentService commentService;

	@Autowired
	private CommentJpaRepository commentJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("루트 댓글 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void rootCommentQueryCountStaysBounded() {
		User owner = saveUser("owner");
		User commenter1 = saveUser("commenter1");
		User commenter2 = saveUser("commenter2");
		User commenter3 = saveUser("commenter3");
		Diary diary = diaryJpaRepository.save(new Diary("comment-diary", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));

		Comment root1 = commentJpaRepository.save(new Comment("comment-1", commenter1.getId(), diary.getId()));
		Comment root2 = commentJpaRepository.save(new Comment("comment-2", commenter2.getId(), diary.getId()));
		Comment root3 = commentJpaRepository.save(new Comment("comment-3", commenter3.getId(), diary.getId()));
		saveReply("reply-1", commenter2.getId(), diary.getId(), root1);
		saveReply("reply-2", commenter3.getId(), diary.getId(), root2);
		saveReply("reply-3", commenter1.getId(), diary.getId(), root3);

		long oneItemQueries = measurePreparedStatements(() ->
				commentService.getRootCommentsByDiaryId(diary.getId(), PageRequest.of(0, 1), REQUEST_META_INFO));
		long threeItemQueries = measurePreparedStatements(() ->
				commentService.getRootCommentsByDiaryId(diary.getId(), PageRequest.of(0, 3), REQUEST_META_INFO));

		assertThat(threeItemQueries)
				.as("댓글 row 수가 늘어도 사용자/답글 수 조회를 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("대댓글 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void replyQueryCountStaysBounded() {
		User owner = saveUser("reply-owner");
		User replier1 = saveUser("replier1");
		User replier2 = saveUser("replier2");
		User replier3 = saveUser("replier3");
		Diary diary = diaryJpaRepository.save(new Diary("reply-diary", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));
		Comment parent = commentJpaRepository.save(new Comment("parent", owner.getId(), diary.getId()));

		saveReply("reply-1", replier1.getId(), diary.getId(), parent);
		saveReply("reply-2", replier2.getId(), diary.getId(), parent);
		saveReply("reply-3", replier3.getId(), diary.getId(), parent);

		long oneItemQueries = measurePreparedStatements(() ->
				commentService.getRepliesByParentCommentId(parent.getId(), PageRequest.of(0, 1), REQUEST_META_INFO));
		long threeItemQueries = measurePreparedStatements(() ->
				commentService.getRepliesByParentCommentId(parent.getId(), PageRequest.of(0, 3), REQUEST_META_INFO));

		assertThat(threeItemQueries)
				.as("대댓글 row 수가 늘어도 사용자 조회는 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
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
