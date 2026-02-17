package store.piku.back.diary.application.port.in;

import store.piku.back.diary.domain.Diary;

public interface GetDiaryUseCase {
	Diary getDiaryById(Long diaryId);
}
