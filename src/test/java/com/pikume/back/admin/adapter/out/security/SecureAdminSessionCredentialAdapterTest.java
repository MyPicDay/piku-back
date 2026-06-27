package com.pikume.back.admin.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecureAdminSessionCredentialAdapter")
class SecureAdminSessionCredentialAdapterTest {

	private final SecureAdminSessionCredentialAdapter adapter = new SecureAdminSessionCredentialAdapter();

	@Test
	@DisplayName("세션 자격 증명은 매번 다른 256비트 이상 난수로 생성한다")
	void generatesHighEntropyCredentials() {
		String first = adapter.generate();
		String second = adapter.generate();

		assertThat(first).isNotEqualTo(second);
		assertThat(first.length()).isGreaterThanOrEqualTo(43);
		assertThat(second.length()).isGreaterThanOrEqualTo(43);
	}

	@Test
	@DisplayName("원문을 저장하지 않고 SHA-256 해시로 검증한다")
	void hashesAndMatchesCredential() {
		String hash = adapter.hash("raw-credential");

		assertThat(hash).hasSize(64).doesNotContain("raw-credential");
		assertThat(adapter.matches("raw-credential", hash)).isTrue();
		assertThat(adapter.matches("other-credential", hash)).isFalse();
	}
}
