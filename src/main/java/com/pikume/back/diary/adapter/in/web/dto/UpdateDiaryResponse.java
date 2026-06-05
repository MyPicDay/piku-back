package com.pikume.back.diary.adapter.in.web.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

public record UpdateDiaryResponse(
		Long diaryId,
		DiaryVisibility status,
		String content) {
}
