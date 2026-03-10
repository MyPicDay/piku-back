package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.SaveFeedClickPort;
import com.pikume.back.feed.domain.FeedClick;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FeedClickPersistenceAdapter implements LoadFeedClickPort, SaveFeedClickPort {

	private final FeedClickJpaRepository feedClickJpaRepository;

	@Override
	public boolean existsByUserIdAndDiaryId(String userId, Long diaryId) {
		return feedClickJpaRepository.existsByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public List<Long> findClickedDiaryIdsByUserId(String userId) {
		return feedClickJpaRepository.findClickedDiaryIdsByUserId(userId);
	}

	@Override
	public Set<Long> findClickedDiaryIdsByUserIdAndDiaryIds(String userId, List<Long> diaryIds) {
		if (userId == null || userId.isBlank() || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return feedClickJpaRepository.findClickedDiaryIdsByUserIdAndDiaryIdIn(userId, diaryIds);
	}

	@Override
	public FeedClick save(FeedClick feedClick) {
		return feedClickJpaRepository.save(feedClick);
	}
}
