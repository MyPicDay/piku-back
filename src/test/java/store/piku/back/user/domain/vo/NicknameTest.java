package store.piku.back.user.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Nickname Value Object")
class NicknameTest {

	@Nested
	@DisplayName("생성")
	class Creation {

		@Test
		@DisplayName("유효한 닉네임으로 생성할 수 있다")
		void validNickname() {
			Nickname nickname = new Nickname("피쿠유저");
			assertThat(nickname.value()).isEqualTo("피쿠유저");
		}

		@Test
		@DisplayName("null 닉네임은 예외를 발생시킨다")
		void nullNickname() {
			assertThatThrownBy(() -> new Nickname(null))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("빈 닉네임은 예외를 발생시킨다")
		void emptyNickname() {
			assertThatThrownBy(() -> new Nickname(""))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("공백만 있는 닉네임은 예외를 발생시킨다")
		void blankNickname() {
			assertThatThrownBy(() -> new Nickname("   "))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("동등성")
	class Equality {

		@Test
		@DisplayName("같은 값이면 동등하다")
		void equalNicknames() {
			Nickname a = new Nickname("피쿠");
			Nickname b = new Nickname("피쿠");
			assertThat(a).isEqualTo(b);
			assertThat(a.hashCode()).isEqualTo(b.hashCode());
		}

		@Test
		@DisplayName("다른 값이면 동등하지 않다")
		void differentNicknames() {
			Nickname a = new Nickname("피쿠A");
			Nickname b = new Nickname("피쿠B");
			assertThat(a).isNotEqualTo(b);
		}
	}
}
