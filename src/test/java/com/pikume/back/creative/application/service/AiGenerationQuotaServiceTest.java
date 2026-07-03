package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;
import com.pikume.back.creative.application.port.out.AiGenerationQuotaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiGenerationQuotaService")
class AiGenerationQuotaServiceTest {

	@Mock
	private AiGenerationQuotaPort aiGenerationQuotaPort;

	@Test
	@DisplayName("AI 생성 사용량을 생성 전에 선차감한다")
	void consumesGenerationQuotaBeforeGeneration() {
		given(aiGenerationQuotaPort.consumeIfAvailable("ai_generate", "user1", 3))
				.willReturn(Optional.of(1));

		AiGenerationQuotaConsumption result = service().tryConsumeForGeneration("user1");

		assertThat(result.consumed()).isTrue();
		assertThat(result.dailyLimit()).isEqualTo(3);
		assertThat(result.remainingCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("AI 생성 일일 한도에 도달하면 선차감에 실패한다")
	void rejectsConsumptionWhenDailyLimitIsReached() {
		given(aiGenerationQuotaPort.consumeIfAvailable("ai_generate", "user1", 3))
				.willReturn(Optional.empty());

		AiGenerationQuotaConsumption result = service().tryConsumeForGeneration("user1");

		assertThat(result.consumed()).isFalse();
		assertThat(result.dailyLimit()).isEqualTo(3);
		assertThat(result.remainingCount()).isZero();
	}

	@Test
	@DisplayName("AI 생성 실패 시 선차감한 사용량을 취소한다")
	void releasesConsumedGenerationQuota() {
		service().releaseGenerationConsumption("user1");

		then(aiGenerationQuotaPort).should().releaseConsumption("ai_generate", "user1");
	}

	@Test
	@DisplayName("AI 생성 남은 횟수를 현재 사용량에서 계산한다")
	void calculatesRemainingGenerationCountFromUsageCount() {
		given(aiGenerationQuotaPort.getUsageCount("ai_generate", "user1"))
				.willReturn(1);

		int result = service().getRemainingGenerationCount("user1");

		assertThat(result).isEqualTo(2);
	}

	@Test
	@DisplayName("AI 생성 남은 횟수는 음수가 되지 않는다")
	void returnsNonNegativeRemainingGenerationCount() {
		given(aiGenerationQuotaPort.getUsageCount("ai_generate", "user1"))
				.willReturn(4);

		int result = service().getRemainingGenerationCount("user1");

		assertThat(result).isZero();
	}

	@Test
	@DisplayName("AI 생성 일일 한도는 creative 애플리케이션 계층이 소유한다")
	void returnsDailyGenerationLimit() {
		assertThat(service().getDailyGenerationLimit()).isEqualTo(3);
	}

	private AiGenerationQuotaService service() {
		return new AiGenerationQuotaService(aiGenerationQuotaPort);
	}
}
