package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.social.application.port.out.LoadCommentListViewPort;
import com.pikume.back.social.application.readmodel.CommentListView;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentListViewPersistenceAdapter implements LoadCommentListViewPort {

	private final CommentJpaRepository commentJpaRepository;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;

	@Override
	public PageResult<CommentListView> loadRootCommentsByDiaryId(Long diaryId, PageQuery pageQuery) {
		org.springframework.data.domain.Page<Comment> rootComments =
				commentJpaRepository.findVisibleRootCommentsByDiaryId(diaryId, SpringPageMapper.toPageable(pageQuery));
		Map<String, UserSummaryView> usersById = loadUsers(rootComments.getContent());
		Map<Long, Integer> replyCountsByParentId = loadReplyCounts(rootComments.getContent());

		List<CommentListView> content = rootComments.getContent().stream()
				.map(comment -> toCommentListView(comment, usersById, replyCountsByParentId))
				.toList();
		return new PageResult<>(content, rootComments.getNumber(), rootComments.getSize(), rootComments.getTotalElements());
	}

	@Override
	public PageResult<CommentListView> loadRepliesByParentCommentId(Long parentCommentId, PageQuery pageQuery) {
		org.springframework.data.domain.Page<Comment> replies =
				commentJpaRepository.findByParentIdAndDeletedAtIsNull(parentCommentId, SpringPageMapper.toPageable(pageQuery));
		Map<String, UserSummaryView> usersById = loadUsers(replies.getContent());

		List<CommentListView> content = replies.getContent().stream()
				.map(comment -> toCommentListView(comment, usersById, Map.of()))
				.toList();
		return new PageResult<>(content, replies.getNumber(), replies.getSize(), replies.getTotalElements());
	}

	private Map<String, UserSummaryView> loadUsers(List<Comment> comments) {
		Set<String> userIds = comments.stream()
				.filter(comment -> !comment.isDeleted())
				.map(Comment::getUserId)
				.collect(Collectors.toSet());
		return queryUserSummaryUseCase.queryUserSummaries(userIds);
	}

	private Map<Long, Integer> loadReplyCounts(List<Comment> comments) {
		Set<Long> commentIds = comments.stream()
				.map(Comment::getId)
				.collect(Collectors.toSet());
		if (commentIds.isEmpty()) {
			return Map.of();
		}

		return commentJpaRepository.countVisibleRepliesByParentIds(commentIds).stream()
				.collect(Collectors.toMap(
						CommentJpaRepository.ReplyCountProjection::getParentId,
						projection -> Math.toIntExact(projection.getReplyCount())));
	}

	private CommentListView toCommentListView(Comment comment, Map<String, UserSummaryView> usersById,
			Map<Long, Integer> replyCountsByParentId) {
		UserSummaryView user = usersById.get(comment.getUserId());
		Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

		return new CommentListView(
				comment.getId(),
				comment.getDiaryId(),
				comment.getUserId(),
				user != null ? user.nickname() : null,
				user != null ? user.avatarPath() : null,
				comment.getContent(),
				parentId,
				replyCountsByParentId.getOrDefault(comment.getId(), 0),
				comment.getCreatedAt(),
				comment.getUpdatedAt(),
				comment.isDeleted());
	}
}
