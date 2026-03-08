package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;

import java.util.List;

public interface LoadFeedCursorCandidatesPort {

	List<FeedCursorCandidate> loadCandidates(String currentUserId, FeedBucket bucket, FeedCursor cursor, int limit);
}
