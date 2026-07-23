package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.social.application.port.out.LoadCommentEngagementPort;
import com.pikume.back.social.application.port.out.LoadCommentThreadsPort;
import com.pikume.back.social.application.port.out.RecordCommentPort;
import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import com.pikume.back.social.domain.comment.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentThreadsPort, LoadCommentEngagementPort, RecordCommentPort {

	private final CommentJpaRepository commentJpaRepository;

	@Override
	public Optional<Comment> loadComment(Long commentId) {
		return commentJpaRepository.findById(commentId);
	}

	@Override
	public PageResult<CommentThreadView> loadRootCommentPage(Long diaryId, PageQuery pageQuery) {
		var page = commentJpaRepository.findVisibleRootCommentsByDiaryId(
				diaryId, SpringPageMapper.toPageable(pageQuery));
		Map<Long, Integer> replyCounts = loadReplyCounts(page.getContent());
		List<CommentThreadView> content = page.getContent().stream()
				.map(comment -> toView(comment, replyCounts.getOrDefault(comment.getId(), 0)))
				.toList();
		return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
	}

	@Override
	public PageResult<CommentThreadView> loadReplyPage(Long parentCommentId, PageQuery pageQuery) {
		var page = commentJpaRepository.findByParentIdAndDeletedAtIsNull(
				parentCommentId, SpringPageMapper.toPageable(pageQuery));
		List<CommentThreadView> content = page.getContent().stream()
				.map(comment -> toView(comment, 0))
				.toList();
		return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
	}

	@Override
	public long countActiveComments(Long diaryId) {
		return commentJpaRepository.countActiveCommentsByDiaryId(diaryId);
	}

	@Override
	public List<DiaryEngagementCount> loadActiveCommentCounts(Collection<Long> diaryIds) {
		return commentJpaRepository.countActiveCommentsByDiaryIds(diaryIds).stream()
				.map(result -> new DiaryEngagementCount(result.getDiaryId(), result.getCommentCount()))
				.toList();
	}

	@Override
	public Set<Long> loadCommentedDiaryIds(String userId, Collection<Long> diaryIds) {
		if (userId == null || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return commentJpaRepository.findCommentedDiaryIdsByUserIdAndDiaryIdIn(userId, diaryIds);
	}

	@Override
	public Comment recordComment(Comment comment) {
		return commentJpaRepository.save(comment);
	}

	private Map<Long, Integer> loadReplyCounts(List<Comment> comments) {
		Set<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toSet());
		if (commentIds.isEmpty()) {
			return Map.of();
		}
		return commentJpaRepository.countVisibleRepliesByParentIds(commentIds).stream()
				.collect(Collectors.toMap(
						CommentJpaRepository.ReplyCountProjection::getParentId,
						projection -> Math.toIntExact(projection.getReplyCount())));
	}

	private CommentThreadView toView(Comment comment, int replyCount) {
		return new CommentThreadView(
				comment.getId(),
				comment.getDiaryId(),
				comment.getUserId(),
				comment.getContent(),
				comment.getParent() != null ? comment.getParent().getId() : null,
				replyCount,
				comment.getCreatedAt(),
				comment.getUpdatedAt(),
				comment.isDeleted());
	}
}
