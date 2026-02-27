package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface LoadVerifiedEmailPort {

	Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type);
}
