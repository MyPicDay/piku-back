package store.piku.back.user.auth.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.user.auth.application.port.out.LoadVerifiedEmailPort;
import store.piku.back.user.auth.application.port.out.SaveVerifiedEmailPort;
import store.piku.back.user.auth.domain.VerifiedEmail;
import store.piku.back.user.auth.domain.vo.VerificationType;

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
