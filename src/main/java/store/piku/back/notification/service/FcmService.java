package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.NonUniqueResultException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import store.piku.back.notification.entity.FcmToken;
import store.piku.back.notification.repository.FcmTokenRepository;

@Service
@RequiredArgsConstructor
@Profile("prod")
@Slf4j
public class FcmService implements NotificationProvider {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public String getTokenByUserId(String userId) {
        return fcmTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("토큰이 존재하지 않습니다."))
                .getToken();
    }


    @Override
    public void saveToken(String userId, String token, String deviceId) {
        try{
            fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresentOrElse(
                    existing -> existing.updateToken(token),
                    () -> fcmTokenRepository.save(new FcmToken(userId, token,deviceId))
            );
        }catch (IncorrectResultSizeDataAccessException | NonUniqueResultException e) {
            log.error("사용자: {} 디바이스: {}에 대한 중복된 토큰이 존재합니다.", userId, deviceId);
        }
    }

    @Override
    public void sendMessage(String targetToken, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setBody(body)
                        .build())
                .build();

        FirebaseMessaging.getInstance().send(message);

    }
}
