package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.auth.domain.Verification;

public interface SaveVerificationPort {

	Verification save(Verification verification);

	void delete(Verification verification);
}
