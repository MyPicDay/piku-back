package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.adapter.in.web.dto.CalendarDiaryResponseDTO;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface GetCalendarUseCase {
	List<CalendarDiaryResponseDTO> findMonthlyDiaries(String userId, int year, int month,
			RequestMetaInfo requestMetaInfo);

	long countDiariesByUserId(String userId);

	List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId);
}
