package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.application.dto.ReissueSessionResult;

public interface ReissueSessionUseCase {
	String reissueAccessToken(String refreshToken);
	ReissueSessionResult reissueSession(String refreshToken);
}
