package store.piku.back.feed.application.port.out;

import store.piku.back.feed.domain.FeedClick;

public interface SaveFeedClickPort {

	FeedClick save(FeedClick feedClick);
}
