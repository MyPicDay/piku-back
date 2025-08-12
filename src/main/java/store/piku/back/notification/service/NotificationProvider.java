package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.transaction.Transactional;

import java.util.Set;

public interface NotificationProvider {

     void sendMessage(String targetToken, String body) throws FirebaseMessagingException;

     @Transactional
     void saveToken(String userId, String token,String deviceId);

     Set<String> getTokenByUserId(String userId);

     void deleteToken(String token);

}
