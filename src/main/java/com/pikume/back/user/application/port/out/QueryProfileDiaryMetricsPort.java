package com.pikume.back.user.application.port.out;

import java.util.List;

public interface QueryProfileDiaryMetricsPort {

	long countVisibleDiaries(String profileUserId, String viewerUserId);

	List<MonthlyDiaryCount> getVisibleMonthlyDiaryCounts(String profileUserId, String viewerUserId);

	record MonthlyDiaryCount(int year, int month, long count) {
	}
}
