package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.port.out.RecordAiPhotoStatisticsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPhotoStatisticsService")
class AiPhotoStatisticsServiceTest {

	@Mock
	private RecordAiPhotoStatisticsPort recordAiPhotoStatisticsPort;

	@Test
	@DisplayName("AI 사진 요청 통계 기록 실패는 호출자에게 전파하지 않는다")
	void recordRequestSwallowsStatisticsFailure() {
		willThrow(new RuntimeException("statistics unavailable"))
				.given(recordAiPhotoStatisticsPort).recordRequest("user1");

		assertThatCode(() -> service().recordRequest("user1"))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("AI 사진 성공과 실패 통계를 creative 포트로 위임한다")
	void recordSuccessAndFailureDelegatesToPort() {
		AiPhotoStatisticsService service = service();

		service.recordSuccess("user1");
		service.recordFailure("user1");

		then(recordAiPhotoStatisticsPort).should().recordSuccess("user1");
		then(recordAiPhotoStatisticsPort).should().recordFailure("user1");
	}

	private AiPhotoStatisticsService service() {
		return new AiPhotoStatisticsService(recordAiPhotoStatisticsPort);
	}
}
