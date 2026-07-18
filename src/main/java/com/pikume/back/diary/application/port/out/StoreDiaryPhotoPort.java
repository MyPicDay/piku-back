package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

public interface StoreDiaryPhotoPort {

	String store(DiaryPhotoUpload photo, DiaryVisibility visibility);
}
