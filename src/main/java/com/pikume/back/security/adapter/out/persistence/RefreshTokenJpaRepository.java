package com.pikume.back.security.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshSessionEntity, String> {
	void deleteByRefreshToken(String refreshToken);

	Optional<RefreshSessionEntity> findByRefreshToken(String refreshToken);
}
