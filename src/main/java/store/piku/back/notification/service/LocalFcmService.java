package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class LocalFcmService implements NotificationProvider {

    @Override
    public List<String> getTokenByUserId(String userId) {
        log.info("토큰 조회(가정)");
        return null;
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
