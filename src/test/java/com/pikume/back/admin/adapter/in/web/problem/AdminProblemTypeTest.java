package com.pikume.back.admin.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminProblemType")
class AdminProblemTypeTest {

	@Test
	@DisplayName("모든 기술 중립 Admin 오류 코드를 기존 Problem type과 상태로 변환한다")
	void mapsEveryAdminErrorWithoutChangingHttpContract() {
		Map<AdminErrorCode, Integer> statuses = Map.ofEntries(
				Map.entry(AdminErrorCode.UNAUTHENTICATED, 401),
				Map.entry(AdminErrorCode.FORBIDDEN, 403),
				Map.entry(AdminErrorCode.CSRF_INVALID, 403),
				Map.entry(AdminErrorCode.NOT_FOUND, 404),
				Map.entry(AdminErrorCode.INVALID_CREDENTIALS, 401),
				Map.entry(AdminErrorCode.TEMPORARY_CREDENTIAL_EXPIRED, 401),
				Map.entry(AdminErrorCode.ACCOUNT_LOCKED, 423),
				Map.entry(AdminErrorCode.DUPLICATE_EMAIL, 409),
				Map.entry(AdminErrorCode.DUPLICATE_LOGIN_ID, 409),
				Map.entry(AdminErrorCode.INVALID_REQUEST, 400),
				Map.entry(AdminErrorCode.OTP_VERIFICATION_FAILED, 401),
				Map.entry(AdminErrorCode.OTP_BLOCKED, 429),
				Map.entry(AdminErrorCode.SESSION_STORE_UNAVAILABLE, 503));

		assertThat(AdminErrorCode.values()).containsExactlyInAnyOrderElementsOf(statuses.keySet());
		statuses.forEach((errorCode, expectedStatus) -> {
			AdminProblemType problemType = AdminProblemType.from(errorCode);
			assertThat(problemType.status().value()).isEqualTo(expectedStatus);
			assertThat(problemType.type().toString())
					.startsWith("https://api.pikume.com/problems/admin/");
		});
	}
}
