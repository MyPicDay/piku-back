package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.in.RecordAdminSecurityEventUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSecurityEventService implements RecordAdminSecurityEventUseCase {

	private final AdminSessionTelemetryPort telemetryPort;

	@Override
	public void recordCsrfRejection(String reason) {
		telemetryPort.csrfRejected(reason);
	}

	@Override
	public void recordOriginRejection() {
		telemetryPort.originRejected();
	}

	@Override
	public void recordSessionStoreUnavailable() {
		telemetryPort.sessionStoreUnavailable();
	}
}
