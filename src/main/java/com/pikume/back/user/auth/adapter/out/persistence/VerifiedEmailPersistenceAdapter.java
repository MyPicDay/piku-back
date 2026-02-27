package com.pikume.back.user.auth.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.user.auth.application.port.out.LoadVerifiedEmailPort;
import com.pikume.back.user.auth.application.port.out.SaveVerifiedEmailPort;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VerifiedEmailPersistenceAdapter implements LoadVerifiedEmailPort, SaveVerifiedEmailPort {

	private final VerifiedEmailJpaRepository verifiedEmailJpaRepository;

	@Override
	public Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type) {
		return verifiedEmailJpaRepository.findTopByEmailAndTypeOrderByVerifiedAtDesc(email, type);
	}

	@Override
	public VerifiedEmail save(VerifiedEmail verifiedEmail) {
		return verifiedEmailJpaRepository.save(verifiedEmail);
	}
}
