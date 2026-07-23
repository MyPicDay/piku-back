package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.out.LoadDiaryDetailPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryDetailQueryService")
class DiaryDetailQueryServiceTest {

	@InjectMocks private DiaryDetailQueryService service;
	@Mock private LoadDiaryDetailPort loadDiaryPort;
	@Mock private DiaryVisibilityPolicy visibilityPolicy;

	@Test
	@DisplayName("조회자에게 보이는 일기만 반환한다")
	void returnsOnlyVisibleDiary() {
		Diary diary = new Diary("content", DiaryVisibility.PUBLIC, LocalDate.now(), "owner");
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(visibilityPolicy.isHiddenFromViewer(diary, "viewer")).willReturn(false);

		assertThat(service.findVisibleDiaryById(1L, "viewer")).isPresent();
		assertThat(service.existsVisibleById(1L, "viewer")).isTrue();
		assertThat(service.findVisibleOwnerUserIdByDiaryId(1L, "viewer")).contains("owner");
	}

	@Test
	@DisplayName("조회자에게 숨겨진 일기는 반환하지 않는다")
	void hidesInvisibleDiary() {
		Diary diary = new Diary("content", DiaryVisibility.PRIVATE, LocalDate.now(), "owner");
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(visibilityPolicy.isHiddenFromViewer(diary, "viewer")).willReturn(true);

		assertThat(service.findVisibleDiaryById(1L, "viewer")).isEmpty();
	}

	@Test
	@DisplayName("다른 Context에 식별자·작성자·익명 여부만 공개한다")
	void exposesMinimalVisibleDiaryReference() {
		Diary diary = new Diary("content", DiaryVisibility.ANONYMOUS, LocalDate.now(), "owner");
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(visibilityPolicy.isHiddenFromViewer(diary, "viewer")).willReturn(false);

		var reference = service.queryVisibleDiaryReference(1L, "viewer").orElseThrow();

		assertThat(reference.ownerUserId()).isEqualTo("owner");
		assertThat(reference.anonymous()).isTrue();
	}
}
