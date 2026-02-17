package store.piku.back.diary.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.application.dto.DiaryMonthCountDTO;
import store.piku.back.diary.application.port.out.LoadDiaryPort;
import store.piku.back.diary.application.port.out.SaveDiaryPort;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;
import store.piku.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryPersistenceAdapter implements LoadDiaryPort, SaveDiaryPort {

	private final DiaryJpaRepository diaryJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;

	@Override
	public Optional<Diary> findById(Long diaryId) {
		return diaryJpaRepository.findById(diaryId);
	}

	@Override
	public List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end) {
		return diaryJpaRepository.findByUserIdAndDateBetween(userId, start, end);
	}

	@Override
	public Optional<Diary> findByUserIdAndDate(String userId, LocalDate date) {
		return diaryJpaRepository.findByUserIdAndDate(userId, date);
	}

	@Override
	public long countByUserId(String userId) {
		return diaryJpaRepository.countByUserId(userId);
	}

	@Override
	public List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, LocalDate monthsAgo) {
		return diaryJpaRepository.countDiariesPerMonth(userId, monthsAgo);
	}

	@Override
	public boolean existsById(Long diaryId) {
		return diaryJpaRepository.existsById(diaryId);
	}

	@Override
	public Optional<Photo> findRepresentPhotoByDiaryId(Long diaryId) {
		return photoJpaRepository.findFirstByDiaryIdAndRepresentIsTrue(diaryId);
	}

	// Feed 관련 (Phase 9 분리 전까지)
	@Override
	public List<Diary> findUnreadFeedsByVisibilityAndUserIds(DiaryVisibility status, List<String> friendIds,
			List<Long> excludeIds) {
		return diaryJpaRepository.findUnreadFeedsByVisibilityAndUserIds(status, friendIds, excludeIds);
	}

	@Override
	public List<Diary> findUnreadPublicFeeds(List<Long> clickedFeedIds) {
		return diaryJpaRepository.findUnreadPublicFeeds(clickedFeedIds);
	}

	@Override
	public List<Diary> findClickedFeedsAfter(List<Long> clickedFeedIds, LocalDateTime threeDaysAgo) {
		return diaryJpaRepository.findClickedFeedsAfter(clickedFeedIds, threeDaysAgo);
	}

	@Override
	public List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status) {
		return diaryJpaRepository.findByStatusOrderByCreatedAtDesc(status);
	}

	@Override
	public List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds) {
		return diaryJpaRepository.findByStatusAndUserIdIn(status, userIds);
	}

	@Override
	public Diary save(Diary diary) {
		return diaryJpaRepository.save(diary);
	}

	@Override
	public Photo savePhoto(Photo photo) {
		return photoJpaRepository.save(photo);
	}
}
