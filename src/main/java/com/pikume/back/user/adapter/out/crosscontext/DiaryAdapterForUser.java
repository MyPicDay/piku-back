package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.user.application.port.out.QueryProfileDiaryMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForUser implements QueryProfileDiaryMetricsPort {

	private final GetCalendarUseCase getCalendarUseCase;

	@Override
	public long queryVisibleDiaryCount(String profileUserId, String viewerUserId) {
		return getCalendarUseCase.countDiariesByUserId(profileUserId, viewerUserId);
	}

	@Override
	public List<MonthlyDiaryCount> queryVisibleMonthlyDiaryCounts(String profileUserId, String viewerUserId) {
		return getCalendarUseCase.getMonthlyDiaryCount(profileUserId, viewerUserId).stream()
				.map(dto -> new MonthlyDiaryCount(dto.getYear(), dto.getMonth(), dto.getCount()))
				.toList();
	}
}
