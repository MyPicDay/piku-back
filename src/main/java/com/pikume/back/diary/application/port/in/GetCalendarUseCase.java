package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.CalendarDiaryView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface GetCalendarUseCase {
	List<CalendarDiaryView> findMonthlyDiaries(String userId, String viewerId, int year, int month,
			RequestMetaInfo requestMetaInfo);

	long countDiariesByUserId(String userId, String viewerId);

	List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId, String viewerId);
}
