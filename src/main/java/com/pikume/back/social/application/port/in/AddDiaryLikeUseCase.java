package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.LikeResult;

public interface AddDiaryLikeUseCase {
	LikeResult addLike(String userId, Long diaryId);
}
