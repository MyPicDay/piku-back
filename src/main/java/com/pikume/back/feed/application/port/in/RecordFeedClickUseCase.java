package com.pikume.back.feed.application.port.in;

public interface RecordFeedClickUseCase {

	void recordClick(String userId, Long diaryId);
}
