package com.pikume.back.diary.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.application.dto.CalendarDiaryView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.VisibleDiaryView;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
	@Mock
	private DiaryVisibilityPolicy diaryVisibilityPolicy;

	private static final String USER_ID = "user-1";

	@Nested
	@DisplayName("visible diary query")
	class VisibleDiaryQuery {

		@Test
		@DisplayName("조회 가능한 일기는 visible query로 반환한다")
		void returnsVisibleDiary() {
			Diary diary = new Diary("공개 일기", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			given(loadDiaryPort.findById(1L)).willReturn(Optional.of(diary));
			given(diaryVisibilityPolicy.isHiddenFromViewer(diary, "viewer-id")).willReturn(false);

			Optional<VisibleDiaryView> result = diaryQueryService.findVisibleDiaryById(1L, "viewer-id");

			assertThat(result).isPresent();
			assertThat(result.get().userId()).isEqualTo(USER_ID);
			assertThat(diaryQueryService.existsVisibleById(1L, "viewer-id")).isTrue();
			assertThat(diaryQueryService.findVisibleOwnerUserIdByDiaryId(1L, "viewer-id")).contains(USER_ID);
		}

		@Test
		@DisplayName("숨겨진 일기는 visible query에서 비어 있다")
		void hidesInvisibleDiary() {
			Diary diary = new Diary("친구 일기", DiaryVisibility.FRIENDS, LocalDate.now(), USER_ID);
			given(loadDiaryPort.findById(1L)).willReturn(Optional.of(diary));
			given(diaryVisibilityPolicy.isHiddenFromViewer(diary, "stranger-id")).willReturn(true);

			assertThat(diaryQueryService.findVisibleDiaryById(1L, "stranger-id")).isEmpty();
			assertThat(diaryQueryService.existsVisibleById(1L, "stranger-id")).isFalse();
			assertThat(diaryQueryService.findVisibleOwnerUserIdByDiaryId(1L, "stranger-id")).isEmpty();
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

			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)),
					any(),
					any()))
					.willReturn(List.of(diary1, diary2));
			given(loadDiaryPort.findRepresentPhotoByDiaryId(any())).willReturn(Optional.empty());

			List<CalendarDiaryView> result = diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 6, requestMetaInfo);

			assertThat(result).hasSize(2);
			assertThat(result.get(0).date()).isEqualTo(date1);
			assertThat(result.get(1).date()).isEqualTo(date2);
		}

		@Test
		@DisplayName("대표 사진이 있으면 URL을 포함한다")
		void includesCoverPhotoUrl() {
			LocalDate date = LocalDate.of(2025, 6, 10);
			Diary diary = new Diary("일기", DiaryVisibility.PUBLIC, date, USER_ID);

			Photo representPhoto = mock(Photo.class);
			given(representPhoto.getUrl()).willReturn("public/cover.jpg");

			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)),
					any(),
					any()))
					.willReturn(List.of(diary));
			given(loadDiaryPort.findRepresentPhotoByDiaryId(any()))
					.willReturn(Optional.of(representPhoto));
			given(photoStoragePort.getPhotoUrl("public/cover.jpg", true))
					.willReturn("https://minio.example.com/public/cover.jpg");

			List<CalendarDiaryView> result = diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 6, requestMetaInfo);

			assertThat(result.get(0).coverPhotoUrl()).isEqualTo("https://minio.example.com/public/cover.jpg");
		}

		@Test
		@DisplayName("일기가 없으면 빈 리스트를 반환한다")
		void returnsEmptyListWhenNoDiaries() {
			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)),
					any(),
					any()))
					.willReturn(List.of());

			List<CalendarDiaryView> result = diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 1, requestMetaInfo);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("비소유자에게는 비공개 일기가 월별 목록에 나타나지 않는다")
		void hidesPrivateDiaryFromOtherViewer() {
			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)),
					any(),
					any()))
					.willReturn(List.of());

			List<CalendarDiaryView> result = diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 6, requestMetaInfo);

			assertThat(result).isEmpty();
			then(loadDiaryPort).should(never()).findRepresentPhotoByDiaryId(any());
		}

		@Test
		@DisplayName("비친구에게는 친구 공개 일기가 월별 목록에 나타나지 않는다")
		void hidesFriendsDiaryFromStranger() {
			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC)),
					any(),
					any()))
					.willReturn(List.of());

			List<CalendarDiaryView> result = diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 6, requestMetaInfo);

			assertThat(result).isEmpty();
			then(loadDiaryPort).should(never()).findRepresentPhotoByDiaryId(any());
		}

		@Test
		@DisplayName("월별 목록 조회는 공개 범위 집합을 한 번만 계산한다")
		void resolvesVisibleStatusesOncePerRequest() {
			LocalDate date1 = LocalDate.of(2025, 6, 1);
			LocalDate date2 = LocalDate.of(2025, 6, 2);
			Diary diary1 = new Diary("일기 1", DiaryVisibility.PUBLIC, date1, USER_ID);
			Diary diary2 = new Diary("일기 2", DiaryVisibility.FRIENDS, date2, USER_ID);

			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.findByUserIdAndStatusesAndDateBetween(
					eq(USER_ID),
					eq(Set.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)),
					any(),
					any()))
					.willReturn(List.of(diary1, diary2));
			given(loadDiaryPort.findRepresentPhotoByDiaryId(any())).willReturn(Optional.empty());

			diaryQueryService.findMonthlyDiaries(USER_ID, "viewer-id", 2025, 6, requestMetaInfo);

			then(diaryVisibilityPolicy).should().visibleStatusesForOwner(USER_ID, "viewer-id");
			then(diaryVisibilityPolicy).should(never()).isHiddenFromViewer(any(Diary.class), eq("viewer-id"));
		}
	}

	@Nested
	@DisplayName("countDiariesByUserId")
	class CountDiariesByUserId {

		@Test
		@DisplayName("사용자의 일기 수를 정상 반환한다")
		void returnsDiaryCount() {
			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.countByUserIdAndStatuses(USER_ID, List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS)))
					.willReturn(42L);

			long count = diaryQueryService.countDiariesByUserId(USER_ID, "viewer-id");

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
			given(diaryVisibilityPolicy.visibleStatusesForOwner(USER_ID, "viewer-id"))
					.willReturn(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS));
			given(loadDiaryPort.countDiariesPerMonth(eq(USER_ID), any(), eq(List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS))))
					.willReturn(expected);

			List<DiaryMonthCountDTO> result = diaryQueryService.getMonthlyDiaryCount(USER_ID, "viewer-id");

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

}
