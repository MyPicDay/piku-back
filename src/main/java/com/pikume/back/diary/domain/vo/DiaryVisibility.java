package com.pikume.back.diary.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DiaryVisibility {
	PUBLIC, PRIVATE, FRIENDS, ANONYMOUS;

	public boolean isPublicStorageScope() {
		return this == PUBLIC || this == ANONYMOUS;
	}

	public boolean requiresPhotoScopeTransitionTo(DiaryVisibility target) {
		if (target == null) {
			throw new IllegalArgumentException("변경할 공개범위는 필수입니다.");
		}
		return isPublicStorageScope() != target.isPublicStorageScope();
	}
}
