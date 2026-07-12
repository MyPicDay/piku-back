package com.pikume.back.user.domain.vo;

import com.pikume.back.user.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object")
class EmailTest {

	@Nested
	@DisplayName("생성")
	class Creation {

		@Test
		@DisplayName("유효한 이메일로 생성할 수 있다")
		void validEmail() {
			Email email = new Email("test@example.com");
			assertThat(email.value()).isEqualTo("test@example.com");
		}

		@Test
		@DisplayName("null 이메일은 예외를 발생시킨다")
		void nullEmail() {
			assertThatThrownBy(() -> new Email(null))
					.isInstanceOf(InvalidEmailException.class);
		}

		@Test
		@DisplayName("빈 이메일은 예외를 발생시킨다")
		void emptyEmail() {
			assertThatThrownBy(() -> new Email(""))
					.isInstanceOf(InvalidEmailException.class);
		}

		@Test
		@DisplayName("잘못된 형식의 이메일은 예외를 발생시킨다")
		void invalidFormat() {
			assertThatThrownBy(() -> new Email("not-an-email"))
					.isInstanceOf(InvalidEmailException.class);
		}

		@Test
		@DisplayName("잘못된 이메일 예외 메시지는 raw 입력을 포함하지 않는다")
		void invalidFormatDoesNotExposeRawValue() {
			String sensitiveEmail = "PRIVATE-INVALID-EMAIL";

			assertThatThrownBy(() -> new Email(sensitiveEmail))
					.isInstanceOf(InvalidEmailException.class)
					.hasMessage("올바르지 않은 이메일 형식입니다.")
					.hasMessageNotContaining(sensitiveEmail);
		}

		@Test
		@DisplayName("@만 있는 이메일은 예외를 발생시킨다")
		void onlyAtSign() {
			assertThatThrownBy(() -> new Email("@"))
					.isInstanceOf(InvalidEmailException.class);
		}
	}

	@Nested
	@DisplayName("동등성")
	class Equality {

		@Test
		@DisplayName("같은 값이면 동등하다")
		void equalEmails() {
			Email a = new Email("test@example.com");
			Email b = new Email("test@example.com");
			assertThat(a).isEqualTo(b);
			assertThat(a.hashCode()).isEqualTo(b.hashCode());
		}
	}
}
