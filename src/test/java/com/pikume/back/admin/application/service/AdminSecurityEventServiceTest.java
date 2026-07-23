package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("AdminSecurityEventService")
class AdminSecurityEventServiceTest {

	private final AdminSessionTelemetryPort telemetryPort = mock(AdminSessionTelemetryPort.class);
	private final AdminSecurityEventService service = new AdminSecurityEventService(telemetryPort);

	@Test
	@DisplayName("Security 관측 사건을 Admin Telemetry 능력으로 전달한다")
	void recordsSecurityEventsThroughAdminTelemetry() {
		service.recordCsrfRejection("cookie_header_mismatch");
		service.recordOriginRejection();
		service.recordSessionStoreUnavailable();

		then(telemetryPort).should().csrfRejected("cookie_header_mismatch");
		then(telemetryPort).should().originRejected();
		then(telemetryPort).should().sessionStoreUnavailable();
	}
}
