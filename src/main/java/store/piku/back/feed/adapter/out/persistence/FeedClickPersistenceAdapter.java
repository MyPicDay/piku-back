package store.piku.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.feed.application.port.out.LoadFeedClickPort;
import store.piku.back.feed.application.port.out.SaveFeedClickPort;
import store.piku.back.feed.domain.FeedClick;

import java.util.List;

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
	public FeedClick save(FeedClick feedClick) {
		return feedClickJpaRepository.save(feedClick);
	}
}
