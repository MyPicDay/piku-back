package com.pikume.back.feed.application.port.out;

import java.util.List;

public interface LoadFeedClickPort {

	boolean existsByUserIdAndDiaryId(String userId, Long diaryId);

	List<Long> findClickedDiaryIdsByUserId(String userId);
}
