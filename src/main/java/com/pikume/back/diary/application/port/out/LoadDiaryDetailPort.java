package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;

import java.util.Optional;

public interface LoadDiaryDetailPort {

	Optional<Diary> findActiveById(Long diaryId);
}
