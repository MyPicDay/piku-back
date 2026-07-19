package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.feed.application.port.in.RecordFeedClickUseCase;
import com.pikume.back.feed.application.port.out.LoadFeedClickHistoryPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryDetailPort;
import com.pikume.back.feed.application.port.out.RecordFeedClickPort;
import com.pikume.back.feed.application.port.out.RecordFeedClickPreferencePort;
import com.pikume.back.feed.domain.FeedClick;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedClickService implements RecordFeedClickUseCase {

	private final LoadFeedDiaryDetailPort loadFeedDiaryDetailPort;
	private final LoadFeedClickHistoryPort loadFeedClickHistoryPort;
	private final RecordFeedClickPort recordFeedClickPort;
	private final RecordFeedClickPreferencePort recordFeedClickPreferencePort;

	@Override
	@Transactional
	public void recordClick(String userId, Long diaryId) {
		if (loadFeedDiaryDetailPort.loadVisibleDiary(diaryId, userId).isEmpty()) {
			return;
		}
		if (loadFeedClickHistoryPort.hasClick(userId, diaryId)) {
			return;
		}

		recordFeedClickPort.record(new FeedClick(userId, diaryId));
		updateUserPreferenceOnClick(userId, diaryId);
	}

	private void updateUserPreferenceOnClick(String userId, Long diaryId) {
		try {
			recordFeedClickPreferencePort.recordClickPreference(userId, diaryId);
			log.debug("클릭 기반 선호도 업데이트 - userId: {}, diaryId: {}", userId, diaryId);
		} catch (Exception e) {
			log.warn("선호도 업데이트 실패 - userId: {}, diaryId: {}", userId, diaryId);
		}
	}
}
