package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Photo;

public interface RecordDiaryPhotoPort {

	Photo record(Photo photo);
}
