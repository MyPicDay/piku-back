package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;

public interface SaveDiaryPort {
	Diary save(Diary diary);

	Photo savePhoto(Photo photo);
}
