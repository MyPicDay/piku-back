package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

public interface RelocateDiaryPhotoPort {

	String copyGeneratedImageToPublic(String sourceObjectKey);

	String copyToVisibilityScope(String sourceObjectKey, DiaryVisibility visibility, DiaryPhotoType sourceType);

	void delete(String objectKey);
}
