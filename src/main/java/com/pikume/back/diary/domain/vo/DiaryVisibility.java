package com.pikume.back.diary.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DiaryVisibility {
	PUBLIC, PRIVATE, FRIENDS
}
