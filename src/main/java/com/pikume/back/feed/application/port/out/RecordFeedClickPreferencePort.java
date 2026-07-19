package com.pikume.back.feed.application.port.out;

public interface RecordFeedClickPreferencePort {

	void recordClickPreference(String userId, Long diaryId);
}
