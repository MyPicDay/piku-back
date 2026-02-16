package store.piku.back.security.application.port.in;

public interface ReissueTokenUseCase {

	/**
	 * Refresh Token으로 Access Token을 재발급합니다.
	 *
	 * @param refreshToken 쿠키에서 추출한 Refresh Token
	 * @return 새로운 Access Token, 실패 시 null
	 */
	String reissueAccessToken(String refreshToken);
}
