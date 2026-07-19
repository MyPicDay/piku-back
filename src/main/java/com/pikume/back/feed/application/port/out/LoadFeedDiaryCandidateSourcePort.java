package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadFeedDiaryCandidateSourcePort {

	List<Long> loadDiaryIdsByVisibilityAndAuthors(
			FeedVisibility visibility,
			List<String> authorIds,
			int limit);

	List<Long> loadDiaryIdsByVisibility(
			FeedVisibility visibility,
			String excludedAuthorId,
			int limit);

	Map<Long, FeedDiaryCandidateView> loadCandidateDetails(Set<Long> diaryIds);
}
