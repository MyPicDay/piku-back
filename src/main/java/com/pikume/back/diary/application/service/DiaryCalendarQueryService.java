package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.CalendarDiaryView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryCalendarPort;
import com.pikume.back.diary.application.port.out.ResolveDiaryPhotoUrlPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryCalendarQueryService implements GetCalendarUseCase {

	private final LoadDiaryCalendarPort loadDiaryPort;
	private final ResolveDiaryPhotoUrlPort photoUrlPort;
	private final DiaryVisibilityPolicy visibilityPolicy;

	@Override
	public List<CalendarDiaryView> findMonthlyDiaries(String userId, String viewerId, int year, int month) {
		if (year < 1 || month < 1 || month > 12) {
			throw new DiaryInvalidRequestException("조회 연월이 올바르지 않습니다.");
		}
		YearMonth yearMonth = YearMonth.of(year, month);
		Set<DiaryVisibility> statuses = Set.copyOf(visibilityPolicy.visibleStatusesForOwner(userId, viewerId));
		List<Diary> diaries = loadDiaryPort.findByOwnerAndStatusesAndDateBetween(
				userId,
				statuses,
				yearMonth.atDay(1),
				yearMonth.atEndOfMonth());
		if (diaries.isEmpty()) {
			return List.of();
		}
		Set<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toSet());
		Map<Long, DiaryPhotoRow> representativePhotos = loadDiaryPort
				.findRepresentativePhotosByDiaryIds(diaryIds).stream()
				.collect(Collectors.toMap(DiaryPhotoRow::diaryId, row -> row, (left, right) -> left));
		return diaries.stream()
				.map(diary -> new CalendarDiaryView(
						diary.getId(),
						resolveRepresentativePhoto(representativePhotos.get(diary.getId())),
						diary.getDate()))
				.toList();
	}

	private String resolveRepresentativePhoto(DiaryPhotoRow photo) {
		return photo == null ? null : photoUrlPort.resolve(photo.displayObjectKey());
	}

	@Override
	public long countDiariesByUserId(String userId, String viewerId) {
		return loadDiaryPort.countByOwnerAndStatuses(userId, visibilityPolicy.visibleStatusesForOwner(userId, viewerId));
	}

	@Override
	public List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId, String viewerId) {
		return loadDiaryPort.countByOwnerAndStatusesPerMonth(
				profileId,
				visibilityPolicy.visibleStatusesForOwner(profileId, viewerId));
	}
}
