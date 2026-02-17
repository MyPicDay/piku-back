package store.piku.back.diary.application.port.in;

import store.piku.back.diary.adapter.in.web.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.application.dto.DiaryMonthCountDTO;
import store.piku.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface GetCalendarUseCase {
	List<CalendarDiaryResponseDTO> findMonthlyDiaries(String userId, int year, int month,
			RequestMetaInfo requestMetaInfo);

	long countDiariesByUserId(String userId);

	List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId);
}
