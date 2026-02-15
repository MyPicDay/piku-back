package store.piku.back.user.application.port.in;

/**
 * 닉네임 중복 확인 유스케이스 (Inbound Port)
 */
public interface CheckNicknameUseCase {

	/**
	 * 닉네임 사용 가능 여부를 확인하고 점유합니다.
	 *
	 * @param nickname 확인할 닉네임
	 * @param userId   요청자 사용자 ID
	 * @return 사용 가능(점유 성공) 여부
	 */
	boolean checkAvailability(String nickname, String userId);
}
