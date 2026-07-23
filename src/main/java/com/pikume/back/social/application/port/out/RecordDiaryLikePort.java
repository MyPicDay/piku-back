package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.like.Like;

public interface RecordDiaryLikePort {
	Like recordLike(Like like);
}
