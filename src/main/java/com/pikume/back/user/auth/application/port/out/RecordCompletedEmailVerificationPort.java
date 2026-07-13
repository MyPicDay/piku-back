package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.auth.domain.VerifiedEmail;

public interface RecordCompletedEmailVerificationPort {

	VerifiedEmail recordCompletedVerification(VerifiedEmail verifiedEmail);
}
