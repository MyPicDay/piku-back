package com.pikume.back.user.auth.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.user.auth.application.port.out.LoadCompletedEmailVerificationPort;
import com.pikume.back.user.auth.application.port.out.RecordCompletedEmailVerificationPort;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VerifiedEmailPersistenceAdapter implements LoadCompletedEmailVerificationPort,
		RecordCompletedEmailVerificationPort {

	private final VerifiedEmailJpaRepository verifiedEmailJpaRepository;

	@Override
	public Optional<VerifiedEmail> loadLatestVerification(String email, VerificationType type) {
		return verifiedEmailJpaRepository.findTopByEmailAndTypeOrderByVerifiedAtDesc(email, type);
	}

	@Override
	public VerifiedEmail recordCompletedVerification(VerifiedEmail verifiedEmail) {
		return verifiedEmailJpaRepository.save(verifiedEmail);
	}
}
