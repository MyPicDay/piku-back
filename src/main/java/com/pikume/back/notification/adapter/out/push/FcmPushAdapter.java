package com.pikume.back.notification.adapter.out.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.NonUniqueResultException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.adapter.out.persistence.FcmTokenJpaRepository;
import com.pikume.back.notification.application.port.out.PushNotificationPort;
import com.pikume.back.notification.domain.FcmToken;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Profile("prod")
@Slf4j
public class FcmPushAdapter implements PushNotificationPort {

	private final FcmTokenJpaRepository fcmTokenJpaRepository;

	@Override
	public Set<String> getTokenByUserId(String userId) {
		return fcmTokenJpaRepository.findAllByUserId(userId).stream()
				.map(FcmToken::getToken)
				.collect(Collectors.toSet());
	}

	@Override
	public void deleteToken(String token) {
		fcmTokenJpaRepository.deleteByToken(token);
		log.info("토큰 삭제: {}", token);
	}

	@Override
	public void deleteTokenForDevice(String userId, String deviceId) {
		fcmTokenJpaRepository.deleteByUserIdAndDeviceId(userId, deviceId);
		log.info("event=fcm_device_token_deleted userId={}", userId);
	}

	@Override
	public void saveToken(String userId, String token, String deviceId) {
		try {
			fcmTokenJpaRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresentOrElse(
					existing -> existing.updateToken(token),
					() -> fcmTokenJpaRepository.save(new FcmToken(userId, token, deviceId)));
		} catch (IncorrectResultSizeDataAccessException | NonUniqueResultException e) {
			log.error("event=fcm_token_duplicate_detected userId={}", userId);
		}
	}

	@Override
	public void sendMessage(String targetToken, String body) throws FirebaseMessagingException {
		Message message = Message.builder()
				.setToken(targetToken)
				.putData("title", "PikU 알림")
				.putData("body", body)
				.putData("url", "/notifications")
				.build();

		FirebaseMessaging.getInstance().send(message);
	}
}
