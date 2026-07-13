package com.pikume.back.notification.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.exception.NotificationStreamSendException;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@DisplayName("SseEmitterAdapter")
class SseEmitterAdapterTest {

	@Test
	@DisplayName("동일 사용자의 모든 연결에 알림을 전송한다")
	void sendToUserSendsMessageToAllUserConnections() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection firstConnection = mock(NotificationStreamConnection.class);
		NotificationStreamConnection secondConnection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("first-emitter-id", "user-id", firstConnection);
		adapter.save("second-emitter-id", "user-id", secondConnection);

		adapter.sendToUser("user-id", message);

		then(firstConnection).should().send(message);
		then(secondConnection).should().send(message);
	}

	@Test
	@DisplayName("다른 사용자의 연결에는 알림을 전송하지 않는다")
	void sendToUserDoesNotSendMessageToOtherUserConnection() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection targetConnection = mock(NotificationStreamConnection.class);
		NotificationStreamConnection otherUserConnection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("target-emitter-id", "target-user-id", targetConnection);
		adapter.save("other-emitter-id", "other-user-id", otherUserConnection);

		adapter.sendToUser("target-user-id", message);

		then(targetConnection).should().send(message);
		then(otherUserConnection).should(times(0)).send(message);
	}

	@Test
	@DisplayName("사용자의 특정 연결만 삭제한다")
	void deleteRemovesOnlySelectedUserConnection() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection deletedConnection = mock(NotificationStreamConnection.class);
		NotificationStreamConnection remainingConnection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("deleted-emitter-id", "user-id", deletedConnection);
		adapter.save("remaining-emitter-id", "user-id", remainingConnection);

		adapter.delete("user-id", "deleted-emitter-id");
		adapter.sendToUser("user-id", message);

		then(deletedConnection).should(times(0)).send(message);
		then(remainingConnection).should().send(message);
	}

	@Test
	@DisplayName("마지막 연결 삭제 후 같은 사용자의 새 연결을 저장할 수 있다")
	void saveRegistersNewConnectionAfterDeletingLastUserConnection() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection deletedConnection = mock(NotificationStreamConnection.class);
		NotificationStreamConnection newConnection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("deleted-emitter-id", "user-id", deletedConnection);
		adapter.delete("user-id", "deleted-emitter-id");

		adapter.save("new-emitter-id", "user-id", newConnection);
		adapter.sendToUser("user-id", message);

		then(deletedConnection).should(times(0)).send(message);
		then(newConnection).should().send(message);
	}

	@Test
	@DisplayName("예상하지 못한 전송 예외는 숨기지 않는다")
	void sendToUserPropagatesUnexpectedSendFailure() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection connection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		RuntimeException unexpectedFailure = new IllegalStateException("serialization bug");
		adapter.save("emitter-id", "user-id", connection);
		willThrow(unexpectedFailure).given(connection).send(message);

		assertThatThrownBy(() -> adapter.sendToUser("user-id", message))
				.isSameAs(unexpectedFailure);
	}

	@Test
	@DisplayName("스트림 전송 실패는 실패한 연결만 삭제하고 정상 연결을 유지한다")
	void sendToUserDeletesOnlyFailedConnectionWhenStreamSendFails() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection failedConnection = mock(NotificationStreamConnection.class);
		NotificationStreamConnection healthyConnection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("failed-emitter-id", "user-id", failedConnection);
		adapter.save("healthy-emitter-id", "user-id", healthyConnection);
		willThrow(new NotificationStreamSendException("SSE 전송 실패", new IOException("Broken pipe")))
				.given(failedConnection).send(message);

		assertThatCode(() -> adapter.sendToUser("user-id", message))
				.doesNotThrowAnyException();
		then(healthyConnection).should().send(message);
		clearInvocations(failedConnection, healthyConnection);

		adapter.sendToUser("user-id", message);

		then(failedConnection).should(times(0)).send(message);
		then(healthyConnection).should().send(message);
	}
}
