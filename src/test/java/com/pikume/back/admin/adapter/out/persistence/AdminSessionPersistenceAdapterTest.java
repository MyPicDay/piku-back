package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.domain.AdminSession;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSessionPersistenceAdapter")
class AdminSessionPersistenceAdapterTest {

	@Mock
	private AdminSessionJpaRepository adminSessionJpaRepository;
	@Mock
	private AdminSession adminSession;
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	@Test
	@DisplayName("단일 활성 세션 교체 순서를 보장하도록 세션 변경을 즉시 flush한다")
	void savesAndFlushesSessionChanges() {
		given(adminSessionJpaRepository.saveAndFlush(adminSession)).willReturn(adminSession);

		AdminSession saved = adapter().save(adminSession);

		assertThat(saved).isSameAs(adminSession);
		then(adminSessionJpaRepository).should().saveAndFlush(adminSession);
	}

	@Test
	@DisplayName("세션 DB 저장 실패를 전용 저장소 예외로 변환한다")
	void translatesSessionStoreFailure() {
		given(adminSessionJpaRepository.saveAndFlush(adminSession))
				.willThrow(new DataAccessResourceFailureException("db unavailable"));

		assertThatThrownBy(() -> adapter().save(adminSession))
				.isInstanceOf(AdminAuthenticationStoreException.class)
				.hasMessage("관리자 세션 저장소를 사용할 수 없습니다.");
	}

	@Test
	@DisplayName("세션 토큰 해시 DB 조회 지연을 메트릭으로 기록한다")
	void recordsSessionLookupLatency() {
		given(adminSessionJpaRepository.findBySessionTokenHash("session-hash"))
				.willReturn(Optional.of(adminSession));

		assertThat(adapter().findBySessionTokenHash("session-hash")).contains(adminSession);

		assertThat(meterRegistry.timer(
				"admin.session.database.lookup", "operation", "token_hash").count()).isEqualTo(1);
	}

	@Test
	@DisplayName("세션 DB 조회 실패를 전용 저장소 예외로 변환한다")
	void translatesSessionLookupFailure() {
		given(adminSessionJpaRepository.findBySessionTokenHash("session-hash"))
				.willThrow(new DataAccessResourceFailureException("db unavailable"));

		assertThatThrownBy(() -> adapter().findBySessionTokenHash("session-hash"))
				.isInstanceOf(AdminAuthenticationStoreException.class)
				.hasMessage("관리자 세션 저장소를 사용할 수 없습니다.");
	}

	private AdminSessionPersistenceAdapter adapter() {
		return new AdminSessionPersistenceAdapter(adminSessionJpaRepository, meterRegistry);
	}
}
