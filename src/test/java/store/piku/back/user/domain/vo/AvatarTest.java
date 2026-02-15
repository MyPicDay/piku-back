package store.piku.back.user.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Avatar Value Object")
class AvatarTest {

	@Nested
	@DisplayName("isEmpty")
	class IsEmpty {

		@Test
		@DisplayName("null path는 빈 아바타이다")
		void nullPath() {
			Avatar avatar = new Avatar(null);
			assertThat(avatar.isEmpty()).isTrue();
		}

		@Test
		@DisplayName("빈 문자열 path는 빈 아바타이다")
		void emptyPath() {
			Avatar avatar = new Avatar("");
			assertThat(avatar.isEmpty()).isTrue();
		}

		@Test
		@DisplayName("공백만 있는 path는 빈 아바타이다")
		void blankPath() {
			Avatar avatar = new Avatar("   ");
			assertThat(avatar.isEmpty()).isTrue();
		}

		@Test
		@DisplayName("유효한 path가 있으면 빈 아바타가 아니다")
		void validPath() {
			Avatar avatar = new Avatar("characters/fixed/base_image_1.png");
			assertThat(avatar.isEmpty()).isFalse();
		}
	}

	@Nested
	@DisplayName("isSameAs")
	class IsSameAs {

		@Test
		@DisplayName("같은 path의 두 아바타는 같다")
		void samePath() {
			Avatar a = new Avatar("characters/fixed/base_image_1.png");
			Avatar b = new Avatar("characters/fixed/base_image_1.png");
			assertThat(a.isSameAs(b)).isTrue();
		}

		@Test
		@DisplayName("다른 path의 두 아바타는 다르다")
		void differentPath() {
			Avatar a = new Avatar("characters/fixed/base_image_1.png");
			Avatar b = new Avatar("characters/fixed/base_image_2.png");
			assertThat(a.isSameAs(b)).isFalse();
		}

		@Test
		@DisplayName("null 아바타와 비교 시 - 자신이 비어있으면 같다")
		void nullOtherWhenEmpty() {
			Avatar empty = new Avatar(null);
			assertThat(empty.isSameAs(null)).isTrue();
		}

		@Test
		@DisplayName("null 아바타와 비교 시 - 자신이 비어있지 않으면 다르다")
		void nullOtherWhenNotEmpty() {
			Avatar avatar = new Avatar("characters/fixed/base_image_1.png");
			assertThat(avatar.isSameAs(null)).isFalse();
		}

		@Test
		@DisplayName("양쪽 모두 빈 아바타이면 같다")
		void bothEmpty() {
			Avatar a = new Avatar(null);
			Avatar b = new Avatar("");
			assertThat(a.isSameAs(b)).isTrue();
		}

		@Test
		@DisplayName("한쪽만 빈 아바타이면 다르다")
		void oneEmpty() {
			Avatar filled = new Avatar("characters/fixed/base_image_1.png");
			Avatar empty = new Avatar(null);
			assertThat(filled.isSameAs(empty)).isFalse();
		}
	}

	@Nested
	@DisplayName("동등성")
	class Equality {

		@Test
		@DisplayName("같은 path이면 record 동등성으로 같다")
		void equalAvatars() {
			Avatar a = new Avatar("path/image.png");
			Avatar b = new Avatar("path/image.png");
			assertThat(a).isEqualTo(b);
			assertThat(a.hashCode()).isEqualTo(b.hashCode());
		}

		@Test
		@DisplayName("다른 path이면 다르다")
		void differentAvatars() {
			Avatar a = new Avatar("path/a.png");
			Avatar b = new Avatar("path/b.png");
			assertThat(a).isNotEqualTo(b);
		}
	}
}
