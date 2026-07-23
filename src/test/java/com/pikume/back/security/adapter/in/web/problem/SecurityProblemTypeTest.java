package com.pikume.back.security.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityProblemType")
class SecurityProblemTypeTest {

	@Test
	@DisplayName("Admin 공개 오류 코드를 기존 관리자 Problem URI와 상태로 번역한다")
	void mapsAdminErrorsWithoutChangingHttpContract() {
		assertThat(SecurityProblemType.fromAdminErrorCode(AdminErrorCode.UNAUTHENTICATED))
				.satisfies(problem -> {
					assertThat(problem.type().toString())
							.isEqualTo("https://api.pikume.com/problems/admin/unauthenticated");
					assertThat(problem.status().value()).isEqualTo(401);
				});
		assertThat(SecurityProblemType.fromAdminErrorCode(AdminErrorCode.CSRF_INVALID))
				.satisfies(problem -> {
					assertThat(problem.type().toString())
							.isEqualTo("https://api.pikume.com/problems/admin/csrf-invalid");
					assertThat(problem.status().value()).isEqualTo(403);
				});
		assertThat(SecurityProblemType.fromAdminErrorCode(AdminErrorCode.SESSION_STORE_UNAVAILABLE))
				.satisfies(problem -> {
					assertThat(problem.type().toString())
							.isEqualTo("https://api.pikume.com/problems/admin/session-store-unavailable");
					assertThat(problem.status().value()).isEqualTo(503);
				});
	}
}
