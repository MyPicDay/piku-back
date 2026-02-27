package com.pikume.back.notification.application.port.out;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.Set;

public interface PushNotificationPort {

	void sendMessage(String targetToken, String body) throws FirebaseMessagingException;

	void saveToken(String userId, String token, String deviceId);

	Set<String> getTokenByUserId(String userId);

	void deleteToken(String token);
}
