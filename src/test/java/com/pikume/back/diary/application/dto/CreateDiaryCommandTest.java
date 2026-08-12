package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreateDiaryCommand")
class CreateDiaryCommandTest {

	@Test
	@DisplayName("이미지 명령 목록은 필수다")
	void imageInfosIsRequired() {
		assertThatThrownBy(() -> new CreateDiaryCommand(
				DiaryVisibility.PUBLIC,
				"일기",
				null,
				LocalDate.of(2026, 8, 12)))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("이미지 명령 목록의 null 원소를 거부한다")
	void nullImageCommandIsRejected() {
		List<DiaryImageCommand> imageInfos = new ArrayList<>();
		imageInfos.add(null);

		assertThatThrownBy(() -> new CreateDiaryCommand(
				DiaryVisibility.PUBLIC,
				"일기",
				imageInfos,
				LocalDate.of(2026, 8, 12)))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("이미지 명령 목록을 방어적으로 복사해 불변으로 유지한다")
	void imageInfosIsDefensivelyCopied() {
		DiaryImageCommand image = new DiaryImageCommand(DiaryPhotoType.AI_IMAGE, 0, 10L, null);
		List<DiaryImageCommand> imageInfos = new ArrayList<>(List.of(image));
		CreateDiaryCommand command = new CreateDiaryCommand(
				DiaryVisibility.PUBLIC,
				"일기",
				imageInfos,
				LocalDate.of(2026, 8, 12));

		imageInfos.clear();

		assertThat(command.imageInfos()).containsExactly(image);
		assertThatThrownBy(() -> command.imageInfos().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
