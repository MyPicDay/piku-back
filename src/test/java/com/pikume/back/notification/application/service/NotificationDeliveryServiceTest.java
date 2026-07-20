package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.out.DeliverNotificationStreamPort;
import com.pikume.back.notification.application.port.out.DeliverPushNotificationPort;
import com.pikume.back.notification.application.port.out.LoadPushDeliveryTokensPort;
import com.pikume.back.notification.application.port.out.RevokePushTokenPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryService")
class NotificationDeliveryServiceTest {

	@InjectMocks
	private NotificationDeliveryService service;

	@Mock
	private DeliverNotificationStreamPort deliverNotificationStreamPort;
	@Mock
	private LoadPushDeliveryTokensPort loadPushDeliveryTokensPort;
	@Mock
	private DeliverPushNotificationPort deliverPushNotificationPort;
	@Mock
	private RevokePushTokenPort revokePushTokenPort;

	@Test
	@DisplayName("SSE를 먼저 전달한 뒤 모든 Push Token에 전달한다")
	void deliversStreamBeforePushTokens() {
		NotificationDeliveryRequest request = request();
		given(loadPushDeliveryTokensPort.loadPushDeliveryTokens("receiver-id"))
				.willReturn(Set.of("token-1", "token-2"));

		service.deliverNotification(request);

		InOrder order = inOrder(deliverNotificationStreamPort, loadPushDeliveryTokensPort);
		order.verify(deliverNotificationStreamPort)
				.deliverNotificationStream("receiver-id", request.streamMessage());
		order.verify(loadPushDeliveryTokensPort).loadPushDeliveryTokens("receiver-id");
		then(deliverPushNotificationPort).should()
				.deliverPushNotification("token-1", "push-body");
		then(deliverPushNotificationPort).should()
				.deliverPushNotification("token-2", "push-body");
	}

	@Test
	@DisplayName("SSE 실패가 Push 전달을 막지 않는다")
	void continuesPushDeliveryWhenStreamFails() {
		NotificationDeliveryRequest request = request();
		willThrow(new IllegalStateException("stream failure"))
				.given(deliverNotificationStreamPort)
				.deliverNotificationStream("receiver-id", request.streamMessage());
		given(loadPushDeliveryTokensPort.loadPushDeliveryTokens("receiver-id"))
				.willReturn(Set.of("token-1"));

		assertThatCode(() -> service.deliverNotification(request)).doesNotThrowAnyException();

		then(deliverPushNotificationPort).should()
				.deliverPushNotification("token-1", "push-body");
	}

	@Test
	@DisplayName("실패한 Push Token만 해제하고 나머지 Token 전달을 계속한다")
	void revokesOnlyFailedPushTokenAndContinues() {
		NotificationDeliveryRequest request = request();
		given(loadPushDeliveryTokensPort.loadPushDeliveryTokens("receiver-id"))
				.willReturn(Set.of("bad-token", "good-token"));
		willThrow(new IllegalStateException("push failure"))
				.given(deliverPushNotificationPort)
				.deliverPushNotification("bad-token", "push-body");

		assertThatCode(() -> service.deliverNotification(request)).doesNotThrowAnyException();

		then(revokePushTokenPort).should().revokePushToken("bad-token");
		then(deliverPushNotificationPort).should()
				.deliverPushNotification("good-token", "push-body");
	}

	private NotificationDeliveryRequest request() {
		return new NotificationDeliveryRequest(
				1L,
				"receiver-id",
				new NotificationStreamMessage("event-id", null, "payload"),
				"push-body");
	}
}
