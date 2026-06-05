package com.pikume.back.diary.application.dto;

import java.util.List;

public record DiaryGalleryPage<T>(
		List<T> items,
		String nextCursor,
		boolean hasNext
) {
}
