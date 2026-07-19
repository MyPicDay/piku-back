package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.policy.FeedAuthorMaskingPolicy;
import com.pikume.back.feed.application.port.in.QueryFeedDetailUseCase;
import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryDetailPort;
import com.pikume.back.feed.application.port.out.LoadFeedEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedDetailQueryService implements QueryFeedDetailUseCase {

	private final LoadFeedDiaryDetailPort loadFeedDiaryDetailPort;
	private final LoadFeedAuthorsPort loadFeedAuthorsPort;
	private final LoadFeedEngagementPort loadFeedEngagementPort;
	private final FeedAuthorMaskingPolicy feedAuthorMaskingPolicy;

	@Override
	@Transactional(readOnly = true)
	public FeedDiaryResult queryDetail(Long diaryId, String userId) {
		log.info("일기 상세 조회 요청 - diaryId: {}", diaryId);

		FeedDiaryDetailView diary = loadFeedDiaryDetailPort.loadVisibleDiary(diaryId, userId)
				.orElseThrow(FeedDiaryNotFoundException::new);
		FeedAuthorView author = loadAuthor(diary);
		FeedAuthorMaskingPolicy.AuthorPresentation authorPresentation = feedAuthorMaskingPolicy.present(
				diary.status(),
				diary.userId(),
				author,
				null,
				userId);
		FeedEngagementView engagement = loadFeedEngagementPort
				.loadEngagements(userId, List.of(diary.diaryId()))
				.getOrDefault(
						diary.diaryId(),
						new FeedEngagementView(diary.diaryId(), 0L, 0L, false));

		return FeedDiaryResult.builder()
				.diaryId(diary.diaryId())
				.status(diary.status())
				.content(diary.content())
				.imgUrls(diary.imageUrls())
				.date(diary.date())
				.nickname(authorPresentation.nickname())
				.avatar(authorPresentation.avatarUrl())
				.userId(authorPresentation.userId())
				.createdAt(diary.createdAt())
				.friendStatus(authorPresentation.friendStatus())
				.commentCount(engagement.commentCount())
				.likeCount(engagement.likeCount())
				.isLiked(engagement.liked())
				.isOwner(authorPresentation.owner())
				.build();
	}

	private FeedAuthorView loadAuthor(FeedDiaryDetailView diary) {
		if (diary.status() == FeedVisibility.ANONYMOUS) {
			return null;
		}
		Map<String, FeedAuthorView> authors = loadFeedAuthorsPort.loadAuthors(Set.of(diary.userId()));
		return authors.get(diary.userId());
	}
}
