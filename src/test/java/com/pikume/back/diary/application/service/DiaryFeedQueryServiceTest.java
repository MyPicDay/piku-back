package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.dto.DiaryVisibilityScope;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.out.LoadDiaryFeedPort;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryFeedQueryService")
class DiaryFeedQueryServiceTest {

	@InjectMocks private DiaryFeedQueryService service;
	@Mock private LoadDiaryFeedPort loadDiaryPort;
	@Mock private DiaryVisibilityPolicy visibilityPolicy;

	@Test
	@DisplayName("조회 가능한 피드 일기 상세는 대표 사진을 먼저 반환한다")
	void returnsVisibleDiaryDetailWithRepresentativePhotoFirst() {
		Diary diary = new Diary("content", DiaryVisibility.PUBLIC, LocalDate.now(), "owner");
		ReflectionTestUtils.setField(diary, "id", 1L);
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(visibilityPolicy.isHiddenFromViewer(diary, "viewer")).willReturn(false);
		given(loadDiaryPort.findPhotoRowsByDiaryIds(Set.of(1L))).willReturn(List.of(
				new DiaryPhotoRow(1L, "second.jpg", null, false),
				new DiaryPhotoRow(1L, "cover.jpg", null, true)));

		assertThat(service.findVisibleDiaryDetailById(1L, "viewer")).get()
				.satisfies(detail -> {
					assertThat(detail.status()).isEqualTo(DiaryVisibilityScope.PUBLIC);
					assertThat(detail.photos().get(0).path()).isEqualTo("cover.jpg");
				});
	}

	@Test
	@DisplayName("공개 Application 범위를 Diary Domain 공개 범위로 번역한다")
	void translatesPublishedVisibilityScopeToDomainVisibility() {
		given(loadDiaryPort.findRecentIdsByStatusAndUserIds(
				DiaryVisibility.FRIENDS,
				List.of("friend-id"),
				20)).willReturn(List.of(1L));
		given(loadDiaryPort.findRecentIdsByStatusExcludingUser(
				DiaryVisibility.ANONYMOUS,
				"viewer-id",
				10)).willReturn(List.of(2L));

		assertThat(service.findDiaryIdsByStatusAndUserIds(
				DiaryVisibilityScope.FRIENDS,
				List.of("friend-id"),
				20)).containsExactly(1L);
		assertThat(service.findDiaryIdsByStatus(
				DiaryVisibilityScope.ANONYMOUS,
				"viewer-id",
				10)).containsExactly(2L);

		then(loadDiaryPort).should().findRecentIdsByStatusAndUserIds(
				DiaryVisibility.FRIENDS,
				List.of("friend-id"),
				20);
		then(loadDiaryPort).should().findRecentIdsByStatusExcludingUser(
				DiaryVisibility.ANONYMOUS,
				"viewer-id",
				10);
	}
}
