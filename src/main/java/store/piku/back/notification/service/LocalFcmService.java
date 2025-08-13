package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import store.piku.back.notification.entity.FcmToken;
import store.piku.back.notification.repository.FcmTokenRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class LocalFcmService implements NotificationProvider {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public Set<String> getTokenByUserId(String userId) {
        Set<String> tokens = fcmTokenRepository.findAllByUserId(userId)
                .stream()
                .map(FcmToken::getToken)
                .collect(Collectors.toSet());
        return tokens;
    }

    @Override
    public void deleteToken(String token) {
        fcmTokenRepository.deleteByToken(token);
        log.info("토큰 삭제: {}", token);
    }

    @Override
    public void sendMessage(String targetToken,String body) throws FirebaseMessagingException {
        log.info("Firebase 한테 알림 보냄(가정)");
    }

    @Override
    public void saveToken(String userId, String token , String deviceId) {
        log.info("fcm 토큰 저장(가정)");
    }
}
