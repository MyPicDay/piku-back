package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedAuthorView;

import java.util.Map;
import java.util.Set;

public interface LoadFeedAuthorsPort {

	Map<String, FeedAuthorView> loadAuthors(Set<String> userIds);
}
