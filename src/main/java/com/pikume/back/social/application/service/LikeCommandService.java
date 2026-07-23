package com.pikume.back.social.application.service;

import com.pikume.back.social.application.dto.LikeResult;
import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.in.AddDiaryLikeUseCase;
import com.pikume.back.social.application.port.in.RemoveDiaryLikeUseCase;
import com.pikume.back.social.application.port.out.LoadDiaryLikesPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordDiaryLikePort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.domain.like.Like;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeCommandService implements AddDiaryLikeUseCase, RemoveDiaryLikeUseCase {

	private final LoadDiaryLikesPort loadDiaryLikesPort;
	private final RecordDiaryLikePort recordDiaryLikePort;
	private final ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	private final PublishSocialNotificationEventPort publishSocialNotificationEventPort;

	@Override
	@Transactional
	public LikeResult addLike(String userId, Long diaryId) {
		InteractionDiaryView diary = loadVisibleDiary(diaryId, userId);
		var existingLike = loadDiaryLikesPort.loadLikeState(userId, diaryId);
		boolean publishCreated = false;

		if (existingLike.isPresent()) {
			Like like = existingLike.get();
			if (like.isActive()) {
				throw new SocialException(SocialErrorCode.ALREADY_LIKED);
			}
			like.reactivate();
			recordDiaryLikePort.recordLike(like);
		} else {
			recordDiaryLikePort.recordLike(Like.builder().userId(userId).diaryId(diaryId).build());
			publishCreated = !diary.ownerUserId().equals(userId);
		}

		if (publishCreated) {
			publishSocialNotificationEventPort.publish(new SocialNotificationEvent.LikeCreated(
					diary.ownerUserId(), userId, diaryId));
		}
		return new LikeResult(diaryId, loadDiaryLikesPort.countActiveLikes(diaryId), true);
	}

	@Override
	@Transactional
	public LikeResult removeLike(String userId, Long diaryId) {
		loadVisibleDiary(diaryId, userId);
		Like like = loadDiaryLikesPort.loadActiveLike(userId, diaryId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.LIKE_NOT_FOUND));
		like.cancel();
		recordDiaryLikePort.recordLike(like);
		return new LikeResult(diaryId, loadDiaryLikesPort.countActiveLikes(diaryId), false);
	}

	private InteractionDiaryView loadVisibleDiary(Long diaryId, String viewerId) {
		return resolveInteractionDiaryPort.resolveVisibleDiary(diaryId, viewerId)
				.orElseThrow(() -> new SocialException(SocialErrorCode.DIARY_NOT_FOUND));
	}
}
