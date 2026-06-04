package com.pikume.back.diary.adapter.in.web.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

public record DiaryGalleryItemResponse(
		Long diaryId,
		String coverPhotoUrl,
		String date,
		Long imageCount,
		DiaryVisibility status
) {
}
