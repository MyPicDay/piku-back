package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.domain.FeedClick;

public interface SaveFeedClickPort {

	FeedClick save(FeedClick feedClick);
}
