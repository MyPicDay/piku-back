package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

public record UpdateDiaryCommand(
		DiaryVisibility status,
		String content) {

	private static final int MAX_CONTENT_LENGTH = 500;

	public UpdateDiaryCommand {
		if (status == null) {
			throw new IllegalArgumentException("공개범위는 필수입니다.");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("일기 내용은 비어 있을 수 없습니다.");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("일기 내용은 최대 500자까지 입력할 수 있습니다.");
		}
	}
}
