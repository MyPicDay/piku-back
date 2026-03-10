package com.pikume.back.user.adapter.out.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.user.application.port.out.UserDiaryPort;

import java.util.List;

/**
 * 일기 도메인 어댑터
 * User 도메인에서 일기 통계를 조회하기 위한 Anti-Corruption Layer입니다.
 */
@Component
@RequiredArgsConstructor
public class DiaryAdapterForUser implements UserDiaryPort {

	private final GetCalendarUseCase getCalendarUseCase;

	@Override
	public long countDiariesByUserId(String userId, String viewerId) {
		return getCalendarUseCase.countDiariesByUserId(userId, viewerId);
	}

	@Override
	public List<MonthlyDiaryCount> getMonthlyDiaryCount(String userId, String viewerId) {
		List<DiaryMonthCountDTO> dtoList = getCalendarUseCase.getMonthlyDiaryCount(userId, viewerId);
		return dtoList.stream()
				.map(dto -> new MonthlyDiaryCount(dto.getYear(), dto.getMonth(), dto.getCount()))
				.toList();
	}
}
