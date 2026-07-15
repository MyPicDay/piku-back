package com.pikume.back.diary.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DiaryJpaRepository gallery query")
class DiaryJpaRepositoryGalleryTest {

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private PhotoJpaRepository photoJpaRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("갤러리 row는 사진 없는 일기와 허용되지 않은 공개범위를 제외하고 date DESC, id DESC로 반환한다")
	void galleryRowsExcludePhotoLessAndHiddenStatuses() {
		String userId = "user-1";
		saveDiary(userId, "photo-less", DiaryVisibility.PUBLIC, LocalDate.of(2026, 5, 31));
		Diary privateDiary = saveDiary(userId, "private", DiaryVisibility.PRIVATE, LocalDate.of(2026, 5, 31));
		Diary latestDifferentMonth = saveDiary(userId, "latest", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 1));
		Diary olderSameDate = saveDiary(userId, "older", DiaryVisibility.PUBLIC, LocalDate.of(2026, 5, 30));
		Diary newerSameDate = saveDiary(userId, "newer", DiaryVisibility.PUBLIC, LocalDate.of(2026, 5, 30));
		saveRepresentPhoto(privateDiary, "public/user-1/private.jpg");
		saveRepresentPhoto(latestDifferentMonth, "public/user-1/latest-cover.jpg");
		saveRepresentPhoto(olderSameDate, "public/user-1/older-cover.jpg");
		savePhoto(olderSameDate, "user-1/older-second.jpg", 1);
		saveRepresentPhoto(newerSameDate, "public/user-1/newer-cover.jpg");
		flushAndClear();

		List<DiaryGalleryRow> rows = diaryJpaRepository.findGalleryRowsByUserIdAndStatuses(
				userId,
				Set.of(DiaryVisibility.PUBLIC),
				null,
				null,
				PageRequest.of(0, 10));

		assertThat(rows).extracting(DiaryGalleryRow::diaryId)
				.containsExactly(latestDifferentMonth.getId(), newerSameDate.getId(), olderSameDate.getId());
		assertThat(rows.get(0).coverPhotoPath()).isEqualTo("public/user-1/latest-cover.jpg");
		assertThat(rows.get(2).imageCount()).isEqualTo(2L);
	}

	@Test
	@DisplayName("갤러리 cursor는 같은 날짜에서는 더 작은 diaryId부터 이어진다")
	void galleryRowsApplyDateAndDiaryIdCursor() {
		String userId = "user-1";
		Diary olderSameDate = saveDiary(userId, "older", DiaryVisibility.PUBLIC, LocalDate.of(2026, 5, 30));
		Diary newerSameDate = saveDiary(userId, "newer", DiaryVisibility.PUBLIC, LocalDate.of(2026, 5, 30));
		saveRepresentPhoto(olderSameDate, "public/user-1/older-cover.jpg");
		saveRepresentPhoto(newerSameDate, "public/user-1/newer-cover.jpg");
		flushAndClear();

		List<DiaryGalleryRow> rows = diaryJpaRepository.findGalleryRowsByUserIdAndStatuses(
				userId,
				Set.of(DiaryVisibility.PUBLIC),
				LocalDate.of(2026, 5, 30),
				newerSameDate.getId(),
				PageRequest.of(0, 10));

		assertThat(rows).extracting(DiaryGalleryRow::diaryId)
				.containsExactly(olderSameDate.getId());
	}

	@Test
	@DisplayName("갤러리 대표 사진은 최적화 object key를 우선한다")
	void galleryRowsPreferOptimizedCoverPhoto() {
		Diary diary = saveDiary("user-1", "optimized", DiaryVisibility.PUBLIC, LocalDate.of(2026, 7, 15));
		Photo cover = new Photo(diary, "public/original.png", 0);
		cover.updateRepresent(true);
		cover.markOptimizationSucceeded("public/optimized.webp");
		photoJpaRepository.save(cover);
		flushAndClear();

		List<DiaryGalleryRow> rows = diaryJpaRepository.findGalleryRowsByUserIdAndStatuses(
				"user-1",
				Set.of(DiaryVisibility.PUBLIC),
				null,
				null,
				PageRequest.of(0, 10));

		assertThat(rows).singleElement()
				.extracting(DiaryGalleryRow::coverPhotoPath)
				.isEqualTo("public/optimized.webp");
	}

	private Diary saveDiary(String userId, String content, DiaryVisibility visibility, LocalDate date) {
		return diaryJpaRepository.save(new Diary(content, visibility, date, userId));
	}

	private void saveRepresentPhoto(Diary diary, String path) {
		Photo photo = new Photo(diary, path, 0);
		photo.updateRepresent(true);
		photoJpaRepository.save(photo);
	}

	private void savePhoto(Diary diary, String path, int order) {
		photoJpaRepository.save(new Photo(diary, path, order));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
