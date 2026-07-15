package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadDiaryForCommandPort {

	Optional<Diary> findActiveById(Long diaryId);

	Optional<Diary> findActiveByUserIdAndDate(String userId, LocalDate date);

	List<Photo> findPhotosByDiaryId(Long diaryId);
}
