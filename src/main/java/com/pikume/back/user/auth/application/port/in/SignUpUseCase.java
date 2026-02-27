package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.dto.request.SignupRequest;

public interface SignUpUseCase {

	void signup(SignupRequest dto);
}
