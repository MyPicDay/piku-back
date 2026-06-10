package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface LoadDiaryForFeedPort {

	Optional<FeedDiaryDetailView> findVisibleDiaryById(Long diaryId, String viewerId);

	List<Long> findFeedIdsByStatusAndUserIds(FeedVisibility status, List<String> userIds, int limit);

	List<Long> findFeedIdsByStatus(FeedVisibility status, String excludedUserId, int limit);

	Map<Long, FeedDiaryCandidateView> getFeedDiaryCandidates(Set<Long> diaryIds);
}
