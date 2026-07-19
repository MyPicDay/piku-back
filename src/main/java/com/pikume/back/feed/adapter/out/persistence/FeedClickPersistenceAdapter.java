package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.port.out.LoadFeedClickHistoryPort;
import com.pikume.back.feed.application.port.out.RecordFeedClickPort;
import com.pikume.back.feed.domain.FeedClick;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FeedClickPersistenceAdapter implements LoadFeedClickHistoryPort, RecordFeedClickPort {

	private final FeedClickJpaRepository feedClickJpaRepository;

	@Override
	public boolean hasRecordedClick(String userId, Long diaryId) {
		return feedClickJpaRepository.existsByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public Set<Long> loadClickedDiaryIds(String userId, List<Long> diaryIds) {
		if (userId == null || userId.isBlank() || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return feedClickJpaRepository.findClickedDiaryIdsByUserIdAndDiaryIdIn(userId, diaryIds);
	}

	@Override
	public FeedClick record(FeedClick feedClick) {
		return feedClickJpaRepository.save(feedClick);
	}
}
