package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryPhotoType;

public record DiaryImageCommand(
		DiaryPhotoType type,
		Integer order,
		Long aiPhotoId,
		Integer photoIndex) {
}
