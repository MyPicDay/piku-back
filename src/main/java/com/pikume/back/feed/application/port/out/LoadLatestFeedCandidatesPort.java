package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;

import java.util.List;

public interface LoadLatestFeedCandidatesPort {

	List<FeedLatestCursorCandidate> loadCandidates(String currentUserId, List<String> friendUserIds,
			FeedCursor cursor, int limit);
}
