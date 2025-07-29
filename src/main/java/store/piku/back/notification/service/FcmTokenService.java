package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.piku.back.notification.entity.FcmToken;
import store.piku.back.notification.repository.FcmTokenRepository;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;


    public String getTokenByUserId(String userId) {
        return fcmTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("토큰이 존재하지 않습니다."))
                .getToken();
    }


    public void saveToken(String userId, String token) {
        fcmTokenRepository.findByUserId(userId).ifPresentOrElse(
                existing -> existing.updateToken(token),
                () -> fcmTokenRepository.save(new FcmToken(userId, token))
        );
    }

    public void sendMessage(String targetToken,String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        FirebaseMessaging.getInstance().send(message);

    }
}
