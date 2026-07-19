package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.domain.FeedClick;

public interface RecordFeedClickPort {

	FeedClick record(FeedClick feedClick);
}
