package com.pikume.back.admin.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.global.error.ProblemDetailFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("AdminExceptionHandler")
class AdminExceptionHandlerTest {

	private final AdminSessionTelemetryPort telemetryPort = mock(AdminSessionTelemetryPort.class);

	@Test
	@DisplayName("관리자 DB 저장소 장애를 캐시 방지 503 Problem Details로 변환한다")
	void mapsDataStoreFailureToServiceUnavailable() {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST", "/api/admin/auth/csrf");

		ResponseEntity<ProblemDetail> response = handler().handleStoreUnavailable(
				new AdminAuthenticationStoreException("db unavailable"), request);

		assertThat(response.getStatusCode().value()).isEqualTo(503);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getType()).isEqualTo(AdminProblem.SESSION_STORE_UNAVAILABLE.type());
		assertThat(response.getBody().getStatus()).isEqualTo(503);
		assertThat(response.getBody().getDetail()).isEqualTo("관리자 인증 저장소를 확인할 수 없습니다.");
		assertThat(response.getBody().getInstance()).hasToString("/api/admin/auth/csrf");
		then(telemetryPort).should().sessionStoreUnavailable();
	}

	private AdminExceptionHandler handler() {
		return new AdminExceptionHandler(new ProblemDetailFactory(), telemetryPort);
	}
}
