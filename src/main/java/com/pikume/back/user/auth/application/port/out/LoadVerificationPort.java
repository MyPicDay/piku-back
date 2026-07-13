package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface LoadVerificationPort {

	Optional<Verification> loadVerification(String email, VerificationType type);
}
