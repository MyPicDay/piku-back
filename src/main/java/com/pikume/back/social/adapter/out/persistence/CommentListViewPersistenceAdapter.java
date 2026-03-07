package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadCommentListViewPort;
import com.pikume.back.social.application.readmodel.CommentListView;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentListViewPersistenceAdapter implements LoadCommentListViewPort {

	private final CommentJpaRepository commentJpaRepository;
	private final UserJpaRepository userJpaRepository;

	@Override
	public Page<CommentListView> loadRootCommentsByDiaryId(Long diaryId, Pageable pageable) {
		Page<Comment> rootComments = commentJpaRepository.findVisibleRootCommentsByDiaryId(diaryId, pageable);
		Map<String, User> usersById = loadUsers(rootComments.getContent());
		Map<Long, Integer> replyCountsByParentId = loadReplyCounts(rootComments.getContent());

		List<CommentListView> content = rootComments.getContent().stream()
				.map(comment -> toCommentListView(comment, usersById, replyCountsByParentId))
				.toList();
		return new PageImpl<>(content, pageable, rootComments.getTotalElements());
	}

	@Override
	public Page<CommentListView> loadRepliesByParentCommentId(Long parentCommentId, Pageable pageable) {
		Page<Comment> replies = commentJpaRepository.findByParentIdAndDeletedAtIsNull(parentCommentId, pageable);
		Map<String, User> usersById = loadUsers(replies.getContent());

		List<CommentListView> content = replies.getContent().stream()
				.map(comment -> toCommentListView(comment, usersById, Map.of()))
				.toList();
		return new PageImpl<>(content, pageable, replies.getTotalElements());
	}

	private Map<String, User> loadUsers(List<Comment> comments) {
		Set<String> userIds = comments.stream()
				.filter(comment -> !comment.isDeleted())
				.map(Comment::getUserId)
				.collect(Collectors.toSet());
		if (userIds.isEmpty()) {
			return Map.of();
		}

		return userJpaRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
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

	private CommentListView toCommentListView(Comment comment, Map<String, User> usersById,
			Map<Long, Integer> replyCountsByParentId) {
		User user = usersById.get(comment.getUserId());
		Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

		return new CommentListView(
				comment.getId(),
				comment.getDiaryId(),
				comment.getUserId(),
				user != null ? user.getNickname() : null,
				user != null ? user.getAvatar() : null,
				comment.getContent(),
				parentId,
				replyCountsByParentId.getOrDefault(comment.getId(), 0),
				comment.getCreatedAt(),
				comment.getUpdatedAt(),
				comment.isDeleted());
	}
}
