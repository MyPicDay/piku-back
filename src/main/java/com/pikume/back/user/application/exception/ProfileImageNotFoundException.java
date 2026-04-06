package com.pikume.back.user.application.exception;

public class ProfileImageNotFoundException extends RuntimeException {
	public ProfileImageNotFoundException(Long imageId) {
		super("존재하지 않는 프로필 이미지입니다. imageId=" + imageId);
	}
}
