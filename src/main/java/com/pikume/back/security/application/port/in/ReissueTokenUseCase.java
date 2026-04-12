package com.pikume.back.security.application.port.in;

import com.pikume.back.security.application.dto.ReissueResult;

public interface ReissueTokenUseCase {

	/**
	 * Refresh Token으로 Access Token을 재발급합니다.
	 *
	 * @param refreshToken 쿠키에서 추출한 Refresh Token
	 * @return 새로운 Access Token, 실패 시 null
	 */
	String reissueAccessToken(String refreshToken);

	ReissueResult reissueTokens(String refreshToken);
}
