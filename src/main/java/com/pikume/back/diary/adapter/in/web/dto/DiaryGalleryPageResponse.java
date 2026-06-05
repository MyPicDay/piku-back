package com.pikume.back.diary.adapter.in.web.dto;

import java.util.List;

public record DiaryGalleryPageResponse<T>(
		List<T> items,
		String nextCursor,
		boolean hasNext
) {
}
