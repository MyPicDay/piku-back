package store.piku.back.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface NotificationProvider {

    public void sendMessage(String targetToken, String body) throws FirebaseMessagingException;

    void saveToken(String userId, String token);

    public String getTokenByUserId(String userId);

}
