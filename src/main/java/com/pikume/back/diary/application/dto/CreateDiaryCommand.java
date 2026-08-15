package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreateDiaryCommand(
		DiaryVisibility status,
		String content,
		List<DiaryImageCommand> imageInfos,
		LocalDate date) {

	public CreateDiaryCommand {
		Objects.requireNonNull(imageInfos, "imageInfos는 필수입니다.");
		imageInfos = List.copyOf(imageInfos);
	}
}
