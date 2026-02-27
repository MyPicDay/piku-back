package com.pikume.back.security.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.security.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, String> {
	void deleteByRefreshToken(String refreshToken);

	Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
