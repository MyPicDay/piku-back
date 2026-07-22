package com.pikume.back.social.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.policy.AnonymousCommentAccessDecision;
import com.pikume.back.social.application.policy.AnonymousCommentAccessPolicy;
import com.pikume.back.social.application.port.in.QueryCommentEngagementUseCase;
import com.pikume.back.social.application.port.in.QueryCommentPageUseCase;
import com.pikume.back.social.application.port.out.LoadCommentEngagementPort;
import com.pikume.back.social.application.port.out.LoadCommentThreadsPort;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import com.pikume.back.social.domain.comment.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements QueryCommentPageUseCase, QueryCommentEngagementUseCase {

	private static final String ANONYMOUS_NICKNAME = "익명";
	private static final String PRIVATE_ROOT_COMMENT_CONTENT = "비공개 댓글";
	private static final String PRIVATE_REPLY_CONTENT = "비공개 답글";
	private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";

	private final LoadCommentThreadsPort loadCommentThreadsPort;
	private final LoadCommentEngagementPort loadCommentEngagementPort;
	private final ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	private final LoadSocialParticipantProfilesPort loadSocialParticipantProfilesPort;
	private final AnonymousCommentAccessPolicy anonymousCommentAccessPolicy;

	@Override
	public PageResult<CommentListItemResult> queryRootCommentPage(
			Long diaryId, PageQuery pageQuery, String viewerId) {
		InteractionDiaryView diary = loadVisibleDiary(diaryId, viewerId);
		PageResult<CommentThreadView> comments = loadCommentThreadsPort.loadRootCommentPage(diaryId, pageQuery);
		return mapPage(comments, diary, viewerId, null);
	}

	@Override
	public PageResult<CommentListItemResult> queryReplyPage(
			Long parentCommentId, PageQuery pageQuery, String viewerId) {
		Comment parent = loadCommentThreadsPort.loadComment(parentCommentId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.COMMENT_NOT_FOUND));
		InteractionDiaryView diary = loadVisibleDiary(parent.getDiaryId(), viewerId);
		PageResult<CommentThreadView> replies = loadCommentThreadsPort.loadReplyPage(parentCommentId, pageQuery);
		return mapPage(replies, diary, viewerId, parent.getUserId());
	}

	@Override
	public long queryActiveCommentCount(String viewerId, Long diaryId) {
		loadVisibleDiary(diaryId, viewerId);
		return loadCommentEngagementPort.countActiveComments(diaryId);
	}

	@Override
	public Map<Long, Long> queryCommentCounts(List<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}
		return loadCommentEngagementPort.loadActiveCommentCounts(diaryIds).stream()
				.collect(Collectors.toMap(DiaryEngagementCount::diaryId, DiaryEngagementCount::count));
	}

	@Override
	public Set<Long> queryCommentedDiaryIds(String userId, List<Long> diaryIds) {
		if (userId == null || userId.isBlank() || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return loadCommentEngagementPort.loadCommentedDiaryIds(userId, diaryIds);
	}

	private PageResult<CommentListItemResult> mapPage(
			PageResult<CommentThreadView> page,
			InteractionDiaryView diary,
			String viewerId,
			String parentAuthorUserId) {
		Map<String, SocialParticipantProfile> profiles = diary.anonymous()
				? Map.of()
				: loadProfiles(page.content());
		return page.map(comment -> diary.anonymous()
				? toAnonymousResult(comment, diary, viewerId, parentAuthorUserId)
				: toIdentifiedResult(comment, profiles, viewerId));
	}

	private Map<String, SocialParticipantProfile> loadProfiles(List<CommentThreadView> comments) {
		Set<String> authorIds = comments.stream()
				.filter(comment -> !comment.deleted())
				.map(CommentThreadView::authorUserId)
				.collect(Collectors.toSet());
		return loadSocialParticipantProfilesPort.loadProfiles(authorIds);
	}

	private CommentListItemResult toIdentifiedResult(
			CommentThreadView comment,
			Map<String, SocialParticipantProfile> profiles,
			String viewerId) {
		if (comment.deleted()) {
			return result(comment, null, null, null, DELETED_COMMENT_CONTENT, false, false, false);
		}
		SocialParticipantProfile profile = profiles.get(comment.authorUserId());
		boolean own = viewerId != null && Objects.equals(comment.authorUserId(), viewerId);
		return result(
				comment,
				comment.authorUserId(),
				profile != null && profile.nickname() != null ? profile.nickname() : "me",
				profile != null ? profile.avatarUrl() : null,
				comment.content(),
				viewerId != null && comment.parentCommentId() == null,
				own,
				own);
	}

	private CommentListItemResult toAnonymousResult(
			CommentThreadView comment,
			InteractionDiaryView diary,
			String viewerId,
			String parentAuthorUserId) {
		AnonymousCommentAccessDecision decision = anonymousCommentAccessPolicy.decide(
				comment, diary, viewerId, parentAuthorUserId);
		String content;
		if (!decision.contentVisible()) {
			content = comment.parentCommentId() == null
					? PRIVATE_ROOT_COMMENT_CONTENT
					: PRIVATE_REPLY_CONTENT;
		} else {
			content = comment.deleted() ? DELETED_COMMENT_CONTENT : comment.content();
		}
		return result(
				comment,
				null,
				ANONYMOUS_NICKNAME,
				null,
				content,
				decision.replyAllowed(),
				decision.editAllowed(),
				decision.deleteAllowed());
	}

	private CommentListItemResult result(
			CommentThreadView comment,
			String userId,
			String nickname,
			String avatar,
			String content,
			boolean canReply,
			boolean canEdit,
			boolean canDelete) {
		return new CommentListItemResult(
				comment.commentId(),
				comment.diaryId(),
				userId,
				nickname,
				avatar,
				content,
				comment.parentCommentId(),
				comment.createdAt(),
				comment.updatedAt(),
				comment.replyCount(),
				canReply,
				canEdit,
				canDelete);
	}

	private InteractionDiaryView loadVisibleDiary(Long diaryId, String viewerId) {
		return resolveInteractionDiaryPort.resolveVisibleDiary(diaryId, viewerId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.DIARY_NOT_FOUND));
	}
}
