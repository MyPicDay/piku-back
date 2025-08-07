package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;

public interface NotificationProvider {

     void sendMessage(String targetToken, String body) throws FirebaseMessagingException;

     void saveToken(String userId, String token,String deviceId);

     List<String> getTokenByUserId(String userId);

}
