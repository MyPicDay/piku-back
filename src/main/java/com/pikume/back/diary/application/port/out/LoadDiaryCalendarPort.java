package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LoadDiaryCalendarPort {

	List<Diary> findByOwnerAndStatusesAndDateBetween(
			String ownerId,
			Collection<DiaryVisibility> statuses,
			LocalDate start,
			LocalDate end);

	List<DiaryPhotoRow> findRepresentativePhotosByDiaryIds(Collection<Long> diaryIds);

	long countByOwnerAndStatuses(String ownerId, Collection<DiaryVisibility> statuses);

	List<DiaryMonthCountDTO> countByOwnerAndStatusesPerMonth(String ownerId, Collection<DiaryVisibility> statuses);
}
