package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UpdateProfileCommand;
import com.pikume.back.user.application.dto.UpdateProfileResult;

/**
 * 프로필 수정 유스케이스 (Inbound Port)
 */
public interface UpdateProfileUseCase {

	/**
	 * 닉네임 및/또는 캐릭터를 변경합니다.
	 *
	 * @param command 프로필 변경 요청
	 * @return 변경 결과
	 */
	UpdateProfileResult updateProfile(UpdateProfileCommand command);

	/**
	 * 프로필 이미지(아바타)를 변경합니다.
	 *
	 * @param userId  사용자 ID
	 * @param imageId 캐릭터 이미지 ID
	 */
	void updateProfileImage(String userId, Long imageId);
}
