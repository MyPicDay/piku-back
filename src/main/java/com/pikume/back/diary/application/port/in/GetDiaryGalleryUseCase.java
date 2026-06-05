package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.DiaryGalleryItemView;
import com.pikume.back.diary.application.dto.DiaryGalleryPage;

public interface GetDiaryGalleryUseCase {

	DiaryGalleryPage<DiaryGalleryItemView> findGallery(String userId, String viewerId, String cursor, int limit);
}
