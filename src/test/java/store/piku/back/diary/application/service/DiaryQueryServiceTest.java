package store.piku.back.diary.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import store.piku.back.diary.adapter.in.web.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.application.dto.DiaryMonthCountDTO;
import store.piku.back.diary.application.port.out.LoadDiaryPort;
import store.piku.back.diary.application.port.out.PhotoStoragePort;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.global.dto.RequestMetaInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryQueryService")
class DiaryQueryServiceTest {

	@InjectMocks
	private DiaryQueryService diaryQueryService;

	@Mock
	private LoadDiaryPort loadDiaryPort;

	@Mock
	private PhotoStoragePort photoStoragePort;

	private static final String USER_ID = "user-1";

	@Nested
	@DisplayName("getDiaryById")
	class GetDiaryById {

		@Test
		@DisplayName("존재하는 일기를 정상 조회한다")
		void returnsDiaryWhenExists() {
			Diary diary = new Diary("일기 내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			given(loadDiaryPort.findById(1L)).willReturn(Optional.of(diary));

			Diary result = diaryQueryService.getDiaryById(1L);

			assertThat(result.getContent()).isEqualTo("일기 내용");
			assertThat(result.getUserId()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("존재하지 않는 일기 조회 시 예외를 던진다")
		void throwsWhenNotFound() {
			given(loadDiaryPort.findById(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> diaryQueryService.getDiaryById(999L))
					.isInstanceOf(DiaryNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("findMonthlyDiaries")
	class FindMonthlyDiaries {

		private RequestMetaInfo requestMetaInfo;

		@BeforeEach
		void setUp() {
			requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
					"https://localhost:8080/api/diary", "TestAgent", "127.0.0.1");
		}

		@Test
		@DisplayName("월별 일기 목록을 정상 반환한다")
		void returnsMonthlyDiaries() {
			LocalDate date1 = LocalDate.of(2025, 6, 1);
			LocalDate date2 = LocalDate.of(2025, 6, 15);
			Diary diary1 = new Diary("일기 1", DiaryVisibility.PUBLIC, date1, USER_ID);
			Diary diary2 = new Diary("일기 2", DiaryVisibility.FRIENDS, date2, USER_ID);

			given(loadDiaryPort.findByUserIdAndDateBetween(eq(USER_ID), any(), any()))
					.willReturn(List.of(diary1, diary2));
			given(loadDiaryPort.findRepresentPhotoByDiaryId(any())).willReturn(Optional.empty());

			List<CalendarDiaryResponseDTO> result = diaryQueryService.findMonthlyDiaries(USER_ID, 2025, 6, requestMetaInfo);

			assertThat(result).hasSize(2);
			assertThat(result.get(0).getDate()).isEqualTo(date1);
			assertThat(result.get(1).getDate()).isEqualTo(date2);
		}

		@Test
		@DisplayName("대표 사진이 있으면 URL을 포함한다")
		void includesCoverPhotoUrl() {
			LocalDate date = LocalDate.of(2025, 6, 10);
			Diary diary = new Diary("일기", DiaryVisibility.PUBLIC, date, USER_ID);

			Photo representPhoto = mock(Photo.class);
			given(representPhoto.getUrl()).willReturn("public/cover.jpg");

			given(loadDiaryPort.findByUserIdAndDateBetween(eq(USER_ID), any(), any()))
					.willReturn(List.of(diary));
			given(loadDiaryPort.findRepresentPhotoByDiaryId(any()))
					.willReturn(Optional.of(representPhoto));
			given(photoStoragePort.getPhotoUrl("public/cover.jpg", true))
					.willReturn("https://minio.example.com/public/cover.jpg");

			List<CalendarDiaryResponseDTO> result = diaryQueryService.findMonthlyDiaries(USER_ID, 2025, 6, requestMetaInfo);

			assertThat(result.get(0).getCoverPhotoUrl()).isEqualTo("https://minio.example.com/public/cover.jpg");
		}

		@Test
		@DisplayName("일기가 없으면 빈 리스트를 반환한다")
		void returnsEmptyListWhenNoDiaries() {
			given(loadDiaryPort.findByUserIdAndDateBetween(eq(USER_ID), any(), any()))
					.willReturn(List.of());

			List<CalendarDiaryResponseDTO> result = diaryQueryService.findMonthlyDiaries(USER_ID, 2025, 1, requestMetaInfo);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("countDiariesByUserId")
	class CountDiariesByUserId {

		@Test
		@DisplayName("사용자의 일기 수를 정상 반환한다")
		void returnsDiaryCount() {
			given(loadDiaryPort.countByUserId(USER_ID)).willReturn(42L);

			long count = diaryQueryService.countDiariesByUserId(USER_ID);

			assertThat(count).isEqualTo(42L);
		}
	}

	@Nested
	@DisplayName("getMonthlyDiaryCount")
	class GetMonthlyDiaryCount {

		@Test
		@DisplayName("월별 일기 수 통계를 반환한다")
		void returnsMonthlyStats() {
			DiaryMonthCountDTO dto1 = new DiaryMonthCountDTO();
			dto1.setYear(2025);
			dto1.setMonth(6);
			dto1.setCount(4L);
			DiaryMonthCountDTO dto2 = new DiaryMonthCountDTO();
			dto2.setYear(2025);
			dto2.setMonth(7);
			dto2.setCount(13L);
			List<DiaryMonthCountDTO> expected = List.of(dto1, dto2);
			given(loadDiaryPort.countDiariesPerMonth(eq(USER_ID), any())).willReturn(expected);

			List<DiaryMonthCountDTO> result = diaryQueryService.getMonthlyDiaryCount(USER_ID);

			assertThat(result).hasSize(2);
			assertThat(result.get(0).getCount()).isEqualTo(4L);
			assertThat(result.get(1).getCount()).isEqualTo(13L);
		}
	}

	@Nested
	@DisplayName("sortPhotos")
	class SortPhotos {

		private RequestMetaInfo requestMetaInfo;

		@BeforeEach
		void setUp() {
			requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
					"https://localhost:8080/api/diary", "TestAgent", "127.0.0.1");
		}

		@Test
		@DisplayName("대표 사진을 첫번째로 정렬하고 URL을 변환한다")
		void sortsRepresentPhotoFirst() {
			Photo photo1 = mock(Photo.class);
			given(photo1.getRepresent()).willReturn(false);
			given(photo1.getUrl()).willReturn("img1.jpg");

			Photo photo2 = mock(Photo.class);
			given(photo2.getRepresent()).willReturn(true);
			given(photo2.getUrl()).willReturn("img2.jpg");

			given(photoStoragePort.getPhotoUrl("img2.jpg", true)).willReturn("url2");
			given(photoStoragePort.getPhotoUrl("img1.jpg", false)).willReturn("url1");

			List<Photo> photos = new java.util.ArrayList<>(List.of(photo1, photo2));
			List<String> result = diaryQueryService.sortPhotos(photos, requestMetaInfo);

			assertThat(result).containsExactly("url2", "url1");
		}
	}

	@Nested
	@DisplayName("sanitizePageable")
	class SanitizePageable {

		@Test
		@DisplayName("허용된 정렬 필드만 통과시킨다")
		void passesAllowedSortFields() {
			Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
			List<String> allowed = List.of("createdAt", "date");

			Pageable result = diaryQueryService.sanitizePageable(pageable, allowed);

			assertThat(result.getSort().getOrderFor("createdAt")).isNotNull();
		}

		@Test
		@DisplayName("허용되지 않은 정렬 필드는 무시한다")
		void filtersDisallowedSortFields() {
			Pageable pageable = PageRequest.of(0, 10, Sort.by("password").descending());
			List<String> allowed = List.of("createdAt", "date");

			Pageable result = diaryQueryService.sanitizePageable(pageable, allowed);

			assertThat(result.getSort().getOrderFor("password")).isNull();
			assertThat(result.getSort().getOrderFor("createdAt")).isNotNull();
		}

		@Test
		@DisplayName("페이지 크기를 1~100 범위로 제한한다")
		void clampsSizeWithinRange() {
			Pageable tooLarge = PageRequest.of(0, 500, Sort.by("createdAt"));
			List<String> allowed = List.of("createdAt");

			Pageable result = diaryQueryService.sanitizePageable(tooLarge, allowed);

			assertThat(result.getPageSize()).isEqualTo(100);
		}

		@Test
		@DisplayName("페이지 번호 0이면 0을 유지한다")
		void keepsPageZero() {
			Pageable zero = PageRequest.of(0, 10, Sort.by("createdAt"));
			List<String> allowed = List.of("createdAt");

			Pageable result = diaryQueryService.sanitizePageable(zero, allowed);

			assertThat(result.getPageNumber()).isEqualTo(0);
		}
	}
}
