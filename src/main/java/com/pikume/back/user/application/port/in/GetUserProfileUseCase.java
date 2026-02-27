package com.pikume.back.user.application.port.in;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;

/**
 * 사용자 프로필 조회 유스케이스 (Inbound Port)
 */
public interface GetUserProfileUseCase {

	/**
	 * 프로필 미리보기를 조회합니다.
	 */
	ProfilePreviewResult getProfilePreview(String profileId, String currentUserId, RequestMetaInfo requestMetaInfo);

	/**
	 * 사용자 프로필 상세를 조회합니다.
	 */
	UserProfileResult getUserProfile(String profileId, String currentUserId, RequestMetaInfo requestMetaInfo);
}
