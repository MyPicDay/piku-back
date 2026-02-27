package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.domain.Diary;

public interface GetDiaryUseCase {
	Diary getDiaryById(Long diaryId);
}
