package com.pikume.back.diary.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.diary.adapter.in.web.dto.CalendarDiaryResponseDTO;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.diary.application.port.in.GetDiaryUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.exception.DiaryNotFoundException;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryQueryService implements GetDiaryUseCase, GetCalendarUseCase {

	private final LoadDiaryPort loadDiaryPort;
	private final PhotoStoragePort photoStoragePort;

	@Override
	public Diary getDiaryById(Long diaryId) {
		return loadDiaryPort.findById(diaryId)
				.orElseThrow(() -> {
					log.error("일기 ID [{}]에 해당하는 일기를 찾을 수 없습니다.", diaryId);
					return new DiaryNotFoundException();
				});
	}

	@Override
	public List<CalendarDiaryResponseDTO> findMonthlyDiaries(String userId, int year, int month,
			RequestMetaInfo requestMetaInfo) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate startOfMonth = yearMonth.atDay(1);
		LocalDate endOfMonth = yearMonth.atEndOfMonth();

		List<Diary> diaries = loadDiaryPort.findByUserIdAndDateBetween(userId, startOfMonth, endOfMonth);

		return diaries.stream().map(diary -> {
			String coverPhotoUrl = loadDiaryPort.findRepresentPhotoByDiaryId(diary.getId())
					.map(Photo::getUrl)
					.map(url -> photoStoragePort.getPhotoUrl(url, true))
					.orElse(null);
			return new CalendarDiaryResponseDTO(diary.getId(), coverPhotoUrl, diary.getDate());
		}).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public long countDiariesByUserId(String userId) {
		log.info("사용자 ID: {} 의 일기 개수 조회 요청", userId);
		return loadDiaryPort.countByUserId(userId);
	}

	@Override
	public List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId) {
		LocalDate monthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
		return loadDiaryPort.countDiariesPerMonth(profileId, monthsAgo);
	}

	public List<String> sortPhotos(List<Photo> photos, RequestMetaInfo requestMetaInfo) {
		for (int i = 0; i < photos.size(); i++) {
			if (Boolean.TRUE.equals(photos.get(i).getRepresent())) {
				if (i != 0) {
					Photo representPhoto = photos.remove(i);
					photos.add(0, representPhoto);
				}
				break;
			}
		}

		return photos.stream()
				.map(photo -> photoStoragePort.getPhotoUrl(photo.getUrl(), photo.getRepresent()))
				.toList();
	}

	public Pageable sanitizePageable(Pageable pageable, List<String> allowedSortFields) {
		int page = Math.max(pageable.getPageNumber(), 0);
		int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);

		Sort safeSort = Sort.unsorted();

		for (Sort.Order order : pageable.getSort()) {
			String property = order.getProperty();
			if (allowedSortFields.contains(property)) {
				safeSort = safeSort.and(Sort.by(order));
			} else {
				log.warn("정렬 필드 '{}' 은 허용되지 않았습니다. 무시됩니다.", property);
			}
		}
		if (!safeSort.isSorted()) {
			safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
		}

		return PageRequest.of(page, size, safeSort);
	}
}
