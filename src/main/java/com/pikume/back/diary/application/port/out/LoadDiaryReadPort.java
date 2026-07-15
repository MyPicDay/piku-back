package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.domain.Diary;

import java.util.Collection;
import java.util.List;

public interface LoadDiaryReadPort {

	List<Diary> findActiveByIds(Collection<Long> diaryIds);

	List<DiaryPhotoRow> findPhotoRowsByDiaryIds(Collection<Long> diaryIds);
}
