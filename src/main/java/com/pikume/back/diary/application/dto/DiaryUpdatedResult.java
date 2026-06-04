package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

public record DiaryUpdatedResult(
		Long diaryId,
		DiaryVisibility status,
		String content) {
}
