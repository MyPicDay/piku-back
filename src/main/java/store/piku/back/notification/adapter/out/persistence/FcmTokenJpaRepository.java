package store.piku.back.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.notification.domain.FcmToken;

import java.util.List;
import java.util.Optional;

public interface FcmTokenJpaRepository extends JpaRepository<FcmToken, Long> {

	Optional<FcmToken> findByUserIdAndDeviceId(String userId, String deviceId);

	List<FcmToken> findAllByUserId(String userId);

	void deleteByToken(String token);
}
