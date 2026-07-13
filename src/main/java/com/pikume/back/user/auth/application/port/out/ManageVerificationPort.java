package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.auth.domain.Verification;

public interface ManageVerificationPort {

	Verification storeVerification(Verification verification);

	void removeVerification(Verification verification);
}
