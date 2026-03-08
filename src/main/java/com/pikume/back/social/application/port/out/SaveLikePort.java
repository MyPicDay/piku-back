package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.like.Like;

public interface SaveLikePort {

	Like saveAndFlush(Like like);
}
