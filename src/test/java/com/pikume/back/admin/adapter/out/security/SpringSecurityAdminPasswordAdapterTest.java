package com.pikume.back.admin.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("SpringSecurityAdminPasswordAdapter")
class SpringSecurityAdminPasswordAdapterTest {

	@Test
	@DisplayName("관리자 비밀번호 인코딩과 검증을 Spring Security 어댑터에 위임한다")
	void delegatesEncodingAndMatching() {
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		given(passwordEncoder.encode("raw-password")).willReturn("encoded-password");
		given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(true);
		SpringSecurityAdminPasswordAdapter adapter = new SpringSecurityAdminPasswordAdapter(passwordEncoder);

		assertThat(adapter.encode("raw-password")).isEqualTo("encoded-password");
		assertThat(adapter.matches("raw-password", "encoded-password")).isTrue();
		then(passwordEncoder).should().encode("raw-password");
		then(passwordEncoder).should().matches("raw-password", "encoded-password");
	}
}
