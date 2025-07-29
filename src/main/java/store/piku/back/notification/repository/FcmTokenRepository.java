package store.piku.back.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.notification.entity.FcmToken;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByUserId(String userId);
}
