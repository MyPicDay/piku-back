package com.pikume.back.diary.application.dto;

public record DiaryPhotoRow(Long diaryId, String originalObjectKey, String optimizedObjectKey, boolean represent) {

	public String displayObjectKey() {
		return optimizedObjectKey != null && !optimizedObjectKey.isBlank() ? optimizedObjectKey : originalObjectKey;
	}
}
