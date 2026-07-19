package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadFeedDiaryCandidateSourcePort {

	List<Long> loadRecentDiaryIdsByVisibilityAndAuthors(
			FeedVisibility visibility,
			List<String> authorIds,
			int limit);

	List<Long> loadRecentDiaryIdsByVisibility(
			FeedVisibility visibility,
			int limit);

	List<Long> loadRecentDiaryIdsByVisibilityExcludingAuthor(
			FeedVisibility visibility,
			String excludedAuthorId,
			int limit);

	Map<Long, FeedDiaryCandidateView> loadCandidateAttributes(Set<Long> diaryIds);
}
