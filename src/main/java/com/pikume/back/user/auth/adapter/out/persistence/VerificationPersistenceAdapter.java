package com.pikume.back.user.auth.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.user.auth.application.port.out.LoadVerificationPort;
import com.pikume.back.user.auth.application.port.out.SaveVerificationPort;
import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VerificationPersistenceAdapter implements LoadVerificationPort, SaveVerificationPort {

	private final VerificationJpaRepository verificationJpaRepository;

	@Override
	public Optional<Verification> findByEmailAndType(String email, VerificationType type) {
		return verificationJpaRepository.findByEmailAndType(email, type);
	}

	@Override
	public Verification save(Verification verification) {
		return verificationJpaRepository.save(verification);
	}

	@Override
	public void delete(Verification verification) {
		verificationJpaRepository.delete(verification);
	}
}
