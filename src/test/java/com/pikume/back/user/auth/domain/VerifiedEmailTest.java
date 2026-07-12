package com.pikume.back.user.auth.domain;

import com.pikume.back.user.auth.domain.vo.VerificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerifiedEmail")
class VerifiedEmailTest {

	@Test
	@DisplayName("이메일과 인증 목적을 함께 보호한다")
	void protectsEmailAndPurpose() {
		VerifiedEmail verifiedEmail = new VerifiedEmail("user@example.com", VerificationType.SIGN_UP);

		assertThat(verifiedEmail.isFor("user@example.com", VerificationType.SIGN_UP)).isTrue();
		assertThat(verifiedEmail.isFor("user@example.com", VerificationType.PASSWORD_RESET)).isFalse();
	}

	@Test
	@DisplayName("검증 완료 기록은 한 번만 사용할 수 있다")
	void canBeUsedOnlyOnce() {
		VerifiedEmail verifiedEmail = new VerifiedEmail("user@example.com", VerificationType.SIGN_UP);

		verifiedEmail.markUsed();

		assertThat(verifiedEmail.isUsed()).isTrue();
		assertThatThrownBy(verifiedEmail::markUsed).isInstanceOf(IllegalStateException.class);
	}
}
