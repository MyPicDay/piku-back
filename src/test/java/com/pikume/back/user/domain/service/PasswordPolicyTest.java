package com.pikume.back.user.domain.service;

import com.pikume.back.user.domain.exception.InvalidPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

	private final PasswordPolicy policy = new PasswordPolicy();

	@Test
	@DisplayName("영문, 숫자와 특수문자로 구성되고 특수문자를 포함한 비밀번호를 허용한다")
	void acceptsValidPassword() {
		assertThatCode(() -> policy.validate("abc@123")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("비어 있는 비밀번호를 거부한다")
	void rejectsBlankPassword() {
		assertThatThrownBy(() -> policy.validate(""))
				.isInstanceOf(InvalidPasswordException.class)
				.hasMessage("비밀번호는 필수 값입니다.");
	}

	@Test
	@DisplayName("특수문자가 없는 비밀번호를 거부한다")
	void rejectsPasswordWithoutSpecialCharacter() {
		assertThatThrownBy(() -> policy.validate("plainPassword"))
				.isInstanceOf(InvalidPasswordException.class)
				.hasMessage("올바르지 않은 비밀번호 형식입니다.");
	}

	@Test
	@DisplayName("허용되지 않은 문자가 포함된 비밀번호를 거부한다")
	void rejectsPasswordWithUnsupportedCharacter() {
		assertThatThrownBy(() -> policy.validate("abc!한글"))
				.isInstanceOf(InvalidPasswordException.class)
				.hasMessage("올바르지 않은 비밀번호 형식입니다.");
	}
}
