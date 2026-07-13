package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;

/**
 * 사용자 프로필 조회 유스케이스 (Inbound Port)
 */
public interface QueryUserProfileUseCase {

	/**
	 * 프로필 미리보기를 조회합니다.
	 */
	ProfilePreviewResult queryProfilePreview(String profileId, String currentUserId);

	/**
	 * 사용자 프로필 상세를 조회합니다.
	 */
	UserProfileResult queryUserProfile(String profileId, String currentUserId);
}
