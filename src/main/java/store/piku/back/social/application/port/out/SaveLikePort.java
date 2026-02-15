package store.piku.back.social.application.port.out;

import store.piku.back.social.domain.like.Like;

public interface SaveLikePort {

	Like save(Like like);
}
