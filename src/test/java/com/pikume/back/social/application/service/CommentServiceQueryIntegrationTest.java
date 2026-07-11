package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;
import java.util.Map;

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
				commentService.getRootCommentsByDiaryId(diary.getId(), PageQuery.of(0, 1), owner.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				commentService.getRootCommentsByDiaryId(diary.getId(), PageQuery.of(0, 3), owner.getId()));

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
				commentService.getRepliesByParentCommentId(parent.getId(), PageQuery.of(0, 1), owner.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				commentService.getRepliesByParentCommentId(parent.getId(), PageQuery.of(0, 3), owner.getId()));

		assertThat(threeItemQueries)
				.as("대댓글 row 수가 늘어도 사용자 조회는 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("댓글 수 집계는 삭제된 댓글을 제외하고 활성 댓글만 센다")
	void commentCountsExcludeDeletedComments() {
		User owner = saveUser("count-owner");
		User commenter = saveUser("count-commenter");
		Diary diary = diaryJpaRepository.save(new Diary("count-diary", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));
		Diary deletedOnlyDiary = diaryJpaRepository.save(new Diary("deleted-only-diary", DiaryVisibility.PUBLIC,
				LocalDate.now(), owner.getId()));

		Comment activeRoot = commentJpaRepository.save(new Comment("active-root", commenter.getId(), diary.getId()));
		Comment deletedRoot = commentJpaRepository.save(new Comment("deleted-root", commenter.getId(), diary.getId()));
		deletedRoot.delete();
		commentJpaRepository.save(deletedRoot);
		saveReply("active-reply", commenter.getId(), diary.getId(), activeRoot);
		Comment deletedReply = saveReply("deleted-reply", commenter.getId(), diary.getId(), activeRoot);
		deletedReply.delete();
		commentJpaRepository.save(deletedReply);
		saveReply("active-reply-on-deleted-root", commenter.getId(), diary.getId(), deletedRoot);
		Comment deletedOnly = commentJpaRepository.save(new Comment("deleted-only", commenter.getId(), deletedOnlyDiary.getId()));
		deletedOnly.delete();
		commentJpaRepository.save(deletedOnly);
		flushAndClear();

		long count = commentService.countActiveCommentsByDiaryId(owner.getId(), diary.getId());
		Map<Long, Long> countsByDiaryId = commentService.getCommentCountsForDiaries(
				java.util.List.of(diary.getId(), deletedOnlyDiary.getId()));

		assertThat(count).isEqualTo(3L);
		assertThat(countsByDiaryId).containsEntry(diary.getId(), 3L);
		assertThat(countsByDiaryId).doesNotContainKey(deletedOnlyDiary.getId());
	}

	@Test
	@DisplayName("삭제된 루트 댓글은 활성 답글이 있을 때만 목록에 placeholder로 남는다")
	void rootCommentListKeepsDeletedParentOnlyWhenActiveReplyExists() {
		User owner = saveUser("root-policy-owner");
		User commenter = saveUser("root-policy-commenter");
		Diary diary = diaryJpaRepository.save(new Diary("root-policy-diary", DiaryVisibility.PUBLIC,
				LocalDate.now(), owner.getId()));
		Comment activeRoot = commentJpaRepository.save(new Comment("active-root", commenter.getId(), diary.getId()));
		Comment deletedRootWithReply = commentJpaRepository.save(new Comment("deleted-root-with-reply",
				commenter.getId(), diary.getId()));
		deletedRootWithReply.delete();
		commentJpaRepository.save(deletedRootWithReply);
		Comment deletedRootWithoutReply = commentJpaRepository.save(new Comment("deleted-root-without-reply",
				commenter.getId(), diary.getId()));
		deletedRootWithoutReply.delete();
		commentJpaRepository.save(deletedRootWithoutReply);
		saveReply("active-reply", commenter.getId(), diary.getId(), deletedRootWithReply);
		flushAndClear();

		PageResult<CommentListItemResult> response = commentService.getRootCommentsByDiaryId(
				diary.getId(), PageQuery.of(0, 10), owner.getId());

		assertThat(response.getContent()).extracting(CommentListItemResult::id)
				.containsExactlyInAnyOrder(activeRoot.getId(), deletedRootWithReply.getId());
		assertThat(response.getContent()).noneMatch(comment -> comment.id().equals(deletedRootWithoutReply.getId()));
		CommentListItemResult deletedPlaceholder = response.getContent().stream()
				.filter(comment -> comment.id().equals(deletedRootWithReply.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(deletedPlaceholder.content()).isEqualTo("삭제된 댓글입니다.");
		assertThat(deletedPlaceholder.userId()).isNull();
		assertThat(deletedPlaceholder.replyCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("대댓글 목록은 삭제된 답글을 제외한다")
	void replyListExcludesDeletedReplies() {
		User owner = saveUser("reply-policy-owner");
		User commenter = saveUser("reply-policy-commenter");
		Diary diary = diaryJpaRepository.save(new Diary("reply-policy-diary", DiaryVisibility.PUBLIC,
				LocalDate.now(), owner.getId()));
		Comment parent = commentJpaRepository.save(new Comment("parent", commenter.getId(), diary.getId()));
		Comment activeReply = saveReply("active-reply", commenter.getId(), diary.getId(), parent);
		Comment deletedReply = saveReply("deleted-reply", commenter.getId(), diary.getId(), parent);
		deletedReply.delete();
		commentJpaRepository.save(deletedReply);
		flushAndClear();

		PageResult<CommentListItemResult> response = commentService.getRepliesByParentCommentId(
				parent.getId(), PageQuery.of(0, 10), owner.getId());

		assertThat(response.getContent()).extracting(CommentListItemResult::id)
				.containsExactly(activeReply.getId());
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
