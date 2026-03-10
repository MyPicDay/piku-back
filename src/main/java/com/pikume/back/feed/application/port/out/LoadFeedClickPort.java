package com.pikume.back.feed.application.port.out;

import java.util.List;
import java.util.Set;

public interface LoadFeedClickPort {

	boolean existsByUserIdAndDiaryId(String userId, Long diaryId);

	List<Long> findClickedDiaryIdsByUserId(String userId);

	Set<Long> findClickedDiaryIdsByUserIdAndDiaryIds(String userId, List<Long> diaryIds);
}
