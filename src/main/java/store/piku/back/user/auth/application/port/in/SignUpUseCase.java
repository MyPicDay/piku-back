package store.piku.back.user.auth.application.port.in;

import store.piku.back.user.auth.dto.request.SignupRequest;

public interface SignUpUseCase {

	void signup(SignupRequest dto);
}
