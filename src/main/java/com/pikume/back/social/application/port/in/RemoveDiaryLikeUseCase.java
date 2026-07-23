package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.LikeResult;

public interface RemoveDiaryLikeUseCase {
	LikeResult removeLike(String userId, Long diaryId);
}
