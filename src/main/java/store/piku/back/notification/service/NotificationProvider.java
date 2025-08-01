package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface NotificationProvider {

     void sendMessage(String targetToken, String body) throws FirebaseMessagingException;

     void saveToken(String userId, String token,String deviceId);

     String getTokenByUserId(String userId);

}
