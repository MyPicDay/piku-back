package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.dto.DiaryVisibilityScope;
import com.pikume.back.diary.application.port.out.LoadDiaryReadPort;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryReadQueryService")
class DiaryReadQueryServiceTest {

	@InjectMocks private DiaryReadQueryService service;
	@Mock private LoadDiaryReadPort loadDiaryPort;

	@Test
	@DisplayName("사진 조회는 최적화 object key를 우선한다")
	void photosPreferOptimizedObjectKey() {
		given(loadDiaryPort.findPhotoRowsByDiaryIds(Set.of(1L))).willReturn(List.of(
				new DiaryPhotoRow(1L, "photo.png", "photo.webp", true)));

		assertThat(service.getDiaryPhotos(Set.of(1L))).singleElement()
				.satisfies(photo -> assertThat(photo.path()).isEqualTo("photo.webp"));
		assertThat(service.getRepresentPhotoPaths(Set.of(1L))).containsEntry(1L, "photo.webp");
	}

	@Test
	@DisplayName("일기 요약은 Domain 공개 범위를 공개 Application 값으로 번역한다")
	void summaryTranslatesDomainVisibilityToPublishedScope() {
		Diary diary = new Diary(
				"content",
				DiaryVisibility.ANONYMOUS,
				LocalDate.of(2026, 7, 23),
				"owner-id");
		ReflectionTestUtils.setField(diary, "id", 1L);
		given(loadDiaryPort.findActiveByIds(Set.of(1L))).willReturn(List.of(diary));

		assertThat(service.getDiarySummaries(Set.of(1L)).get(1L).status())
				.isEqualTo(DiaryVisibilityScope.ANONYMOUS);
	}
}
