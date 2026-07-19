package com.pikume.back.feed.application.port.out;

import java.util.List;
import java.util.Set;

public interface LoadFeedClickHistoryPort {

	boolean hasRecordedClick(String userId, Long diaryId);

	Set<Long> loadClickedDiaryIds(String userId, List<Long> diaryIds);
}
