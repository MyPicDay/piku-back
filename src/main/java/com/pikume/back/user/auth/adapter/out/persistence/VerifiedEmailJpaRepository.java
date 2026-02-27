package com.pikume.back.user.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;

import java.util.Optional;

public interface VerifiedEmailJpaRepository extends JpaRepository<VerifiedEmail, Long> {

	Optional<VerifiedEmail> findTopByEmailAndTypeOrderByVerifiedAtDesc(String email, VerificationType type);
}
