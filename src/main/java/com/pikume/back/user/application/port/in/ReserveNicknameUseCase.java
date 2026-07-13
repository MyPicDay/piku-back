package com.pikume.back.user.application.port.in;

/**
 * 사용 가능한 닉네임 점유 유스케이스 (Inbound Port)
 */
public interface ReserveNicknameUseCase {

	/**
	 * 닉네임 사용 가능 여부를 확인하고 변경 요청자를 위해 점유합니다.
	 *
	 * @param nickname 확인할 닉네임
	 * @param userId   요청자 사용자 ID
	 * @return 사용 가능(점유 성공) 여부
	 */
	boolean reserveIfAvailable(String nickname, String userId);
}
