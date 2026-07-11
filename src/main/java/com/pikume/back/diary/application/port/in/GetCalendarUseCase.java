package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.CalendarDiaryView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;

import java.util.List;

public interface GetCalendarUseCase {
	List<CalendarDiaryView> findMonthlyDiaries(String userId, String viewerId, int year, int month);

	long countDiariesByUserId(String userId, String viewerId);

	List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId, String viewerId);
}
