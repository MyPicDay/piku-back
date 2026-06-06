package com.pikume.back.diary.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.diary.application.dto.CalendarDiaryView;
import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryGalleryCursor;
import com.pikume.back.diary.application.dto.DiaryGalleryItemView;
import com.pikume.back.diary.application.dto.DiaryGalleryPage;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.application.dto.DiaryPhotoView;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.VisibleDiaryView;
import com.pikume.back.diary.application.dto.VisibleDiaryDetailView;
import com.pikume.back.diary.application.exception.DiaryNotFoundException;
import com.pikume.back.diary.application.port.in.QueryDiaryFeedUseCase;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.diary.application.port.in.GetDiaryGalleryUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryVisibilityUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryQueryService implements GetCalendarUseCase, QueryDiaryVisibilityUseCase, QueryDiaryReadUseCase,
		QueryDiaryFeedUseCase, GetDiaryGalleryUseCase {

	private final LoadDiaryPort loadDiaryPort;
	private final PhotoStoragePort photoStoragePort;
	private final DiaryVisibilityPolicy diaryVisibilityPolicy;
	private final DiaryGalleryCursorTokenCodec diaryGalleryCursorTokenCodec;

	@Override
	public Optional<VisibleDiaryView> findVisibleDiaryById(Long diaryId, String viewerId) {
		return loadDiaryPort.findById(diaryId)
				.filter(diary -> !diaryVisibilityPolicy.isHiddenFromViewer(diary, viewerId))
				.map(this::toVisibleDiaryView);
	}

	@Override
	public Optional<VisibleDiaryDetailView> findVisibleDiaryDetailById(Long diaryId, String viewerId) {
		return loadDiaryPort.findById(diaryId)
				.filter(diary -> !diaryVisibilityPolicy.isHiddenFromViewer(diary, viewerId))
				.map(diary -> new VisibleDiaryDetailView(
						diary.getId(),
						diary.getUserId(),
						diary.getStatus(),
						diary.getContent(),
						sortPhotoViews(loadDiaryPort.findPhotoRowsByDiaryIds(Set.of(diary.getId()))),
						diary.getDate(),
						diary.getCreatedAt()));
	}

	@Override
	public boolean existsVisibleById(Long diaryId, String viewerId) {
		return findVisibleDiaryById(diaryId, viewerId).isPresent();
	}

	@Override
	public Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId) {
		return findVisibleDiaryById(diaryId, viewerId)
				.map(VisibleDiaryView::userId);
	}

	@Override
	public List<CalendarDiaryView> findMonthlyDiaries(String userId, String viewerId, int year, int month,
			RequestMetaInfo requestMetaInfo) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate startOfMonth = yearMonth.atDay(1);
		LocalDate endOfMonth = yearMonth.atEndOfMonth();
		Set<com.pikume.back.diary.domain.vo.DiaryVisibility> visibleStatuses = Set.copyOf(
				diaryVisibilityPolicy.visibleStatusesForOwner(userId, viewerId));

		List<Diary> diaries = loadDiaryPort.findByUserIdAndStatusesAndDateBetween(userId, visibleStatuses, startOfMonth,
				endOfMonth);

		return diaries.stream()
				.map(diary -> {
			String coverPhotoUrl = loadDiaryPort.findRepresentPhotoByDiaryId(diary.getId())
					.map(Photo::getDisplayUrl)
					.map(url -> photoStoragePort.getPhotoUrl(url, true))
					.orElse(null);
			return new CalendarDiaryView(diary.getId(), coverPhotoUrl, diary.getDate());
		}).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public DiaryGalleryPage<DiaryGalleryItemView> findGallery(String userId, String viewerId, String cursorToken, int limit) {
		DiaryGalleryCursor cursor = diaryGalleryCursorTokenCodec.decode(cursorToken);
		Set<com.pikume.back.diary.domain.vo.DiaryVisibility> visibleStatuses = Set.copyOf(
				diaryVisibilityPolicy.visibleStatusesForOwner(userId, viewerId));

		int fetchLimit = limit + 1;
		List<DiaryGalleryRow> rows = loadDiaryPort.findGalleryRowsByUserIdAndStatuses(
				userId,
				visibleStatuses,
				cursor != null ? cursor.date() : null,
				cursor != null ? cursor.diaryId() : null,
				fetchLimit);

		boolean hasNext = rows.size() > limit;
		List<DiaryGalleryRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
		List<DiaryGalleryItemView> items = pageRows.stream()
				.map(this::toDiaryGalleryItemView)
				.toList();
		String nextCursor = hasNext && !pageRows.isEmpty()
				? diaryGalleryCursorTokenCodec.encode(toGalleryCursor(pageRows.get(pageRows.size() - 1)))
				: null;

		return new DiaryGalleryPage<>(items, nextCursor, hasNext);
	}

	@Override
	public Map<Long, DiarySummaryView> getDiarySummaries(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return java.util.Map.of();
		}

		return loadDiaryPort.findByIds(diaryIds).stream()
				.collect(Collectors.toMap(
						Diary::getId,
						diary -> new DiarySummaryView(
								diary.getId(),
								diary.getUserId(),
								diary.getStatus(),
								diary.getContent(),
								diary.getDate(),
								diary.getCreatedAt()),
						(left, right) -> left));
	}

	@Override
	public List<DiaryPhotoView> getDiaryPhotos(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}

		return loadDiaryPort.findPhotoRowsByDiaryIds(diaryIds).stream()
				.map(photo -> new DiaryPhotoView(
						photo.diaryId(),
						photo.displayUrl(),
						photo.represent()))
				.toList();
	}

	@Override
	public List<Long> findRestorableDiaryIds(List<Long> diaryIds, String viewerId, List<String> friendUserIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}

		Set<String> friendUserIdSet = friendUserIds == null ? Set.of() : Set.copyOf(friendUserIds);
		java.util.Set<Long> restorableIdSet = loadDiaryPort.findByIds(diaryIds).stream()
				.filter(diary -> viewerId == null || !viewerId.equals(diary.getUserId()))
				.filter(diary -> diary.getStatus() == com.pikume.back.diary.domain.vo.DiaryVisibility.PUBLIC
						|| (diary.getStatus() == com.pikume.back.diary.domain.vo.DiaryVisibility.FRIENDS
						&& friendUserIdSet.contains(diary.getUserId())))
				.map(Diary::getId)
				.collect(Collectors.toSet());

		return diaryIds.stream()
				.filter(restorableIdSet::contains)
				.toList();
	}

	@Override
	public List<Long> findDiaryIdsByStatusAndUserIds(com.pikume.back.diary.domain.vo.DiaryVisibility status, List<String> userIds,
			int limit) {
		return loadDiaryPort.findRecentDiaryIdsByStatusAndUserIds(status, userIds, limit);
	}

	@Override
	public List<Long> findDiaryIdsByStatus(com.pikume.back.diary.domain.vo.DiaryVisibility status, String excludedUserId, int limit) {
		return loadDiaryPort.findRecentDiaryIdsByStatusExcludingUser(status, excludedUserId, limit);
	}

	@Override
	public List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(String viewerId, List<String> friendUserIds,
			LocalDateTime cursorCreatedAt, Long cursorDiaryId, int limit) {
		return loadDiaryPort.findLatestVisibleFeedCandidates(viewerId, friendUserIds, cursorCreatedAt, cursorDiaryId, limit);
	}

	@Override
	public Map<Long, String> getRepresentPhotoPaths(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return java.util.Map.of();
		}

		return loadDiaryPort.findPhotoRowsByDiaryIds(diaryIds).stream()
				.filter(LoadDiaryPort.PhotoRow::represent)
				.collect(Collectors.toMap(
						LoadDiaryPort.PhotoRow::diaryId,
						LoadDiaryPort.PhotoRow::displayUrl,
						(left, right) -> left));
	}

	@Override
	@Transactional(readOnly = true)
	public long countDiariesByUserId(String userId, String viewerId) {
		log.info("사용자 ID: {} 의 일기 개수 조회 요청", userId);
		return loadDiaryPort.countByUserIdAndStatuses(userId, diaryVisibilityPolicy.visibleStatusesForOwner(userId, viewerId));
	}

	@Override
	public List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId, String viewerId) {
		LocalDate monthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
		return loadDiaryPort.countDiariesPerMonth(
				profileId,
				monthsAgo,
				diaryVisibilityPolicy.visibleStatusesForOwner(profileId, viewerId));
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
				.map(photo -> photoStoragePort.getPhotoUrl(photo.getDisplayUrl(), photo.getRepresent()))
				.toList();
	}

	private List<DiaryPhotoView> sortPhotoViews(List<LoadDiaryPort.PhotoRow> photoRows) {
		return photoRows.stream()
				.map(row -> new DiaryPhotoView(row.diaryId(), row.displayUrl(), row.represent()))
				.sorted(java.util.Comparator.comparing(DiaryPhotoView::represent).reversed())
				.toList();
	}

	private VisibleDiaryView toVisibleDiaryView(Diary diary) {
		return new VisibleDiaryView(
				diary.getId(),
				diary.getUserId(),
				diary.getStatus(),
				diary.getContent(),
				diary.getDate(),
				diary.getCreatedAt());
	}

	private DiaryGalleryItemView toDiaryGalleryItemView(DiaryGalleryRow row) {
		return new DiaryGalleryItemView(
				row.diaryId(),
				photoStoragePort.getPhotoUrl(row.coverPhotoPath(), true),
				row.date(),
				row.imageCount(),
				row.status());
	}

	private DiaryGalleryCursor toGalleryCursor(DiaryGalleryRow row) {
		return new DiaryGalleryCursor(row.date(), row.diaryId());
	}
}
