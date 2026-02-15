package store.piku.back.user.application.port.out;

import java.util.List;

/**
 * 일기 통계 조회 Outbound Port (타 도메인 의존성 추상화)
 */
public interface UserDiaryPort {

	/**
	 * 사용자의 일기 총 개수를 조회합니다.
	 */
	long countDiariesByUserId(String userId);

	/**
	 * 사용자의 월별 일기 개수를 조회합니다.
	 *
	 * @return 월별 일기 개수 리스트 (year, month, count)
	 */
	List<MonthlyDiaryCount> getMonthlyDiaryCount(String userId);

	/**
	 * 월별 일기 개수 데이터
	 */
	record MonthlyDiaryCount(int year, int month, long count) {
	}
}
