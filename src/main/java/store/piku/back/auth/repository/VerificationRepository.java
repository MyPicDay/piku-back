package store.piku.back.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.auth.entity.Verification;
import store.piku.back.auth.enums.VerificationType;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    Optional<Verification> findByEmailAndType(String email, VerificationType type);
}
