package store.piku.back.user.auth.application.port.out;

import store.piku.back.user.auth.domain.VerifiedEmail;
import store.piku.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface LoadVerifiedEmailPort {

	Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type);
}
