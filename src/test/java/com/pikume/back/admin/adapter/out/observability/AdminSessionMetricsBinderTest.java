package com.pikume.back.admin.adapter.out.observability;

import com.pikume.back.admin.application.port.out.CountAdminSessionsPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("AdminSessionMetricsBinder")
class AdminSessionMetricsBinderTest {

	@Test
	@DisplayName("DB 기준 활성 인증 완료 세션 수를 게이지로 노출한다")
	void exposesActiveAuthenticatedSessionCount() {
		CountAdminSessionsPort countAdminSessionsPort = mock(CountAdminSessionsPort.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		given(countAdminSessionsPort.countActiveAuthenticatedAt(any())).willReturn(2L);

		new AdminSessionMetricsBinder(registry, countAdminSessionsPort);

		assertThat(registry.get("admin.session.active").gauge().value()).isEqualTo(2.0);
		then(countAdminSessionsPort).should().countActiveAuthenticatedAt(any());
	}
}
