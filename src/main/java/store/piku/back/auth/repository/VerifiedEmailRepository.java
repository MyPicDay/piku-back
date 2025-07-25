package store.piku.back.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.auth.entity.VerifiedEmail;
import store.piku.back.auth.enums.VerificationType;

import java.util.Optional;

public interface VerifiedEmailRepository extends JpaRepository<VerifiedEmail, Long> {

    Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type);

}