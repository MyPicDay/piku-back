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
	@DisplayName("스트림 전송 실패는 emitter를 삭제하고 예외로 올리지 않는다")
	void sendToUserDeletesEmitterWithoutThrowingWhenStreamSendFails() {
		SseEmitterAdapter adapter = new SseEmitterAdapter();
		NotificationStreamConnection connection = mock(NotificationStreamConnection.class);
		NotificationStreamMessage message = new NotificationStreamMessage("event-id", null, "data");
		adapter.save("emitter-id", "user-id", connection);
		willThrow(new NotificationStreamSendException("SSE 전송 실패", new IOException("Broken pipe")))
				.given(connection).send(message);

		assertThatCode(() -> adapter.sendToUser("user-id", message))
				.doesNotThrowAnyException();
		clearInvocations(connection);

		adapter.sendToUser("user-id", message);

		then(connection).should(times(0)).send(message);
	}
}
