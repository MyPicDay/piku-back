package store.piku.back.user.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.user.auth.domain.VerifiedEmail;
import store.piku.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface VerifiedEmailJpaRepository extends JpaRepository<VerifiedEmail, Long> {

	Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type);
}
