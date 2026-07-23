package com.pikume.back.admin.application.port.in;

public interface RecordAdminSecurityEventUseCase {

	void recordCsrfRejection(String reason);

	void recordOriginRejection();

	void recordSessionStoreUnavailable();
}
