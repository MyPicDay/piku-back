package store.piku.back.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<store.piku.back.auth.entity.RefreshToken, String> {
    void deleteByRefreshToken(String refreshToken);
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
