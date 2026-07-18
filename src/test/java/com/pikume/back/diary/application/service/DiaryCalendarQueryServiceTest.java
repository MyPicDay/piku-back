package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.out.LoadDiaryCalendarPort;
import com.pikume.back.diary.application.port.out.ResolveDiaryPhotoUrlPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryCalendarQueryService")
class DiaryCalendarQueryServiceTest {

	@InjectMocks private DiaryCalendarQueryService service;
	@Mock private LoadDiaryCalendarPort loadDiaryPort;
	@Mock private ResolveDiaryPhotoUrlPort photoUrlPort;
	@Mock private DiaryVisibilityPolicy visibilityPolicy;

	@Test
	@DisplayName("조회자에게 보이는 월별 일기의 대표 사진을 한 번에 조회한다")
	void returnsVisibleMonthlyDiaries() {
		Diary firstDiary = new Diary("first", DiaryVisibility.PUBLIC, LocalDate.of(2026, 7, 1), "owner");
		Diary secondDiary = new Diary("second", DiaryVisibility.PUBLIC, LocalDate.of(2026, 7, 2), "owner");
		ReflectionTestUtils.setField(firstDiary, "id", 1L);
		ReflectionTestUtils.setField(secondDiary, "id", 2L);
		given(visibilityPolicy.visibleStatusesForOwner("owner", "viewer"))
				.willReturn(List.of(DiaryVisibility.PUBLIC));
		given(loadDiaryPort.findByOwnerAndStatusesAndDateBetween(
				"owner", Set.of(DiaryVisibility.PUBLIC), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
				.willReturn(List.of(firstDiary, secondDiary));
		given(loadDiaryPort.findRepresentativePhotosByDiaryIds(Set.of(1L, 2L)))
				.willReturn(List.of(
						new DiaryPhotoRow(1L, "public/first.jpg", null, true),
						new DiaryPhotoRow(2L, "public/second.jpg", null, true)));
		given(photoUrlPort.resolve("public/first.jpg")).willReturn("https://images/first.jpg");
		given(photoUrlPort.resolve("public/second.jpg")).willReturn("https://images/second.jpg");

		assertThat(service.findMonthlyDiaries("owner", "viewer", 2026, 7))
				.extracting(view -> view.coverPhotoUrl())
				.containsExactly("https://images/first.jpg", "https://images/second.jpg");
		then(loadDiaryPort).should().findRepresentativePhotosByDiaryIds(Set.of(1L, 2L));
	}

	@Test
	@DisplayName("유효하지 않은 연월은 저장소 조회 전에 거부한다")
	void rejectsInvalidYearMonth() {
		assertThatThrownBy(() -> service.findMonthlyDiaries("owner", "viewer", 2026, 13))
				.isInstanceOf(com.pikume.back.diary.application.exception.DiaryInvalidRequestException.class)
				.hasMessageContaining("월");

		then(loadDiaryPort).shouldHaveNoInteractions();
	}
}
