package com.pikume.back.user.auth.application.service;

import com.pikume.back.user.auth.application.port.out.LoadAllowedEmailDomainPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("AllowedEmailQueryService")
class AllowedEmailQueryServiceTest {

	private final LoadAllowedEmailDomainPort port = mock(LoadAllowedEmailDomainPort.class);
	private final AllowedEmailQueryService service = new AllowedEmailQueryService(port);

	@Test
	@DisplayName("이메일 도메인 허용 여부와 목록을 전용 Port로 조회한다")
	void queriesAllowedDomains() {
		given(port.isAllowedEmailDomain("example.com")).willReturn(true);
		given(port.loadAllowedEmailDomains()).willReturn(List.of("example.com"));

		assertThat(service.isEmailAllowed("user@example.com")).isTrue();
		assertThat(service.queryAllowedEmailDomains()).containsExactly("example.com");
	}
}
