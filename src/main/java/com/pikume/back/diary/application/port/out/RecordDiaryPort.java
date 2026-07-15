package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;

public interface RecordDiaryPort {

	Diary record(Diary diary);
}
