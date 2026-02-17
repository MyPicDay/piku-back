package store.piku.back.diary.application.port.out;

import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;

public interface SaveDiaryPort {
	Diary save(Diary diary);

	Photo savePhoto(Photo photo);
}
