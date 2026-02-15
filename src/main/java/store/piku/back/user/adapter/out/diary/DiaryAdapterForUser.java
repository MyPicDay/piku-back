package store.piku.back.user.adapter.out.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.dto.DiaryMonthCountDTO;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.user.application.port.out.UserDiaryPort;

import java.util.List;

/**
 * 일기 도메인 어댑터
 * User 도메인에서 일기 통계를 조회하기 위한 Anti-Corruption Layer입니다.
 */
@Component
@RequiredArgsConstructor
public class DiaryAdapterForUser implements UserDiaryPort {

	private final DiaryService diaryService;

	@Override
	public long countDiariesByUserId(String userId) {
		return diaryService.countDiariesByUserId(userId);
	}

	@Override
	public List<MonthlyDiaryCount> getMonthlyDiaryCount(String userId) {
		List<DiaryMonthCountDTO> dtoList = diaryService.getMonthlyDiaryCount(userId);
		return dtoList.stream()
				.map(dto -> new MonthlyDiaryCount(dto.getYear(), dto.getMonth(), dto.getCount()))
				.toList();
	}
}
