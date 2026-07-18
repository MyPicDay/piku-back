package com.pikume.back.diary.adapter.out.persistence;

import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.PhotoOptimizationStatus;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("PhotoJpaRepository optimization query")
class PhotoJpaRepositoryOptimizationTest {

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private PhotoJpaRepository photoJpaRepository;

	@Test
	@DisplayName("삭제된 일기의 사진은 최적화 대상에서 제외한다")
	void excludesPhotosOwnedByDeletedDiaries() {
		Diary activeDiary = diaryJpaRepository.save(
				new Diary("active", DiaryVisibility.PRIVATE, LocalDate.of(2026, 7, 14), "user-1"));
		Diary deletedDiary = diaryJpaRepository.save(
				new Diary("deleted", DiaryVisibility.PRIVATE, LocalDate.of(2026, 7, 13), "user-1"));
		deletedDiary.delete();
		diaryJpaRepository.save(deletedDiary);
		Photo activePhoto = photoJpaRepository.save(new Photo(activeDiary, "private/active.png", 0));
		photoJpaRepository.save(new Photo(deletedDiary, "private/deleted.png", 0));

		List<PhotoOptimizationTarget> targets = photoJpaRepository.findPendingPhotoOptimizationTargets(
				PhotoOptimizationStatus.PENDING,
				PageRequest.of(0, 10));

		assertThat(targets).extracting(PhotoOptimizationTarget::photoId)
				.containsExactly(activePhoto.getId());
	}

	@Test
	@DisplayName("삭제된 일기의 사진은 일기 조회 결과에서도 제외한다")
	void excludesPhotosOwnedByDeletedDiariesFromReadQueries() {
		Diary activeDiary = diaryJpaRepository.save(
				new Diary("active", DiaryVisibility.PUBLIC, LocalDate.of(2026, 7, 14), "user-1"));
		Diary deletedDiary = diaryJpaRepository.save(
				new Diary("deleted", DiaryVisibility.PUBLIC, LocalDate.of(2026, 7, 13), "user-1"));
		Photo activePhoto = new Photo(activeDiary, "public/active.png", 0);
		activePhoto.updateRepresent(true);
		photoJpaRepository.save(activePhoto);
		Photo deletedPhoto = new Photo(deletedDiary, "public/deleted.png", 0);
		deletedPhoto.updateRepresent(true);
		photoJpaRepository.save(deletedPhoto);
		deletedDiary.delete();
		diaryJpaRepository.saveAndFlush(deletedDiary);

		List<Long> diaryIds = List.of(activeDiary.getId(), deletedDiary.getId());

		assertThat(photoJpaRepository.findByDiaryIds(diaryIds))
				.extracting(photo -> photo.getDiary().getId())
				.containsExactly(activeDiary.getId());
		assertThat(photoJpaRepository.findRepresentPhotoUrlsByDiaryIds(diaryIds))
				.extracting(PhotoJpaRepository.DiaryThumbnailProjection::getDiaryId)
				.containsExactly(activeDiary.getId());
		assertThat(photoJpaRepository.findPhotoRowsByDiaryIds(diaryIds))
				.extracting(PhotoJpaRepository.DiaryPhotoRowProjection::getDiaryId)
				.containsExactly(activeDiary.getId());
	}
}
