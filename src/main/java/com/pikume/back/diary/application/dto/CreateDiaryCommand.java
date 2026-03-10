package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.List;

public record CreateDiaryCommand(
		DiaryVisibility status,
		String content,
		List<DiaryImageCommand> imageInfos,
		LocalDate date) {
}
