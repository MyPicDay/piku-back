package com.pikume.back.notification.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.exception.NotificationStreamSendException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SseEmitterConnection")
class SseEmitterConnectionTest {

	@Test
	@DisplayName("이미 완료된 emitter 전송 실패는 스트림 전송 실패로 변환한다")
	void sendConvertsCompletedEmitterFailure() {
		SseEmitterConnection connection = new SseEmitterConnection(1000L);
		connection.complete();

		assertThatThrownBy(() -> connection.send(new NotificationStreamMessage("event-id", null, "data")))
				.isInstanceOf(NotificationStreamSendException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("예상하지 못한 IllegalStateException은 숨기지 않는다")
	void sendPropagatesUnexpectedIllegalStateException() {
		SseEmitterConnection connection = new SseEmitterConnection(1000L);
		IllegalStateException unexpectedFailure = new IllegalStateException("serialization bug");
		ReflectionTestUtils.setField(connection, "emitter", new ThrowingSseEmitter(unexpectedFailure));

		assertThatThrownBy(() -> connection.send(new NotificationStreamMessage("event-id", null, "data")))
				.isSameAs(unexpectedFailure);
	}

	private static class ThrowingSseEmitter extends SseEmitter {

		private final RuntimeException failure;

		private ThrowingSseEmitter(RuntimeException failure) {
			this.failure = failure;
		}

		@Override
		public void send(SseEventBuilder builder) throws IOException {
			throw failure;
		}
	}
}
