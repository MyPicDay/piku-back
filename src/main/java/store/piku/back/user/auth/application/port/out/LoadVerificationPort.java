package store.piku.back.user.auth.application.port.out;

import store.piku.back.user.auth.domain.Verification;
import store.piku.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface LoadVerificationPort {

	Optional<Verification> findByEmailAndType(String email, VerificationType type);
}
