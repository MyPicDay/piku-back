package com.pikume.back.security.adapter.out.password;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("PasswordProtectionAdapter")
class PasswordProtectionAdapterTest {

	@Test
	@DisplayName("Spring Security 비밀번호 구현을 User 목적 계약으로 감싼다")
	void delegatesPasswordProtection() {
		PasswordEncoder encoder = mock(PasswordEncoder.class);
		given(encoder.encode("raw")).willReturn("protected");
		given(encoder.matches("raw", "protected")).willReturn(true);
		PasswordProtectionAdapter adapter = new PasswordProtectionAdapter(encoder);

		assertThat(adapter.protect("raw")).isEqualTo("protected");
		assertThat(adapter.matches("raw", "protected")).isTrue();
	}
}
