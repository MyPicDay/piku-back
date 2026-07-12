package com.pikume.back.user.domain;

import com.pikume.back.user.domain.vo.Avatar;
import com.pikume.back.user.domain.vo.Email;
import com.pikume.back.user.domain.vo.Nickname;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User")
class UserTest {

	@Test
	@DisplayName("이메일, 닉네임과 아바타를 값 객체 상태로 보유한다")
	void ownsProfileValueObjects() throws NoSuchFieldException {
		assertThat(User.class.getDeclaredField("email").getType()).isEqualTo(Email.class);
		assertThat(User.class.getDeclaredField("nickname").getType()).isEqualTo(Nickname.class);
		assertThat(User.class.getDeclaredField("avatar").getType()).isEqualTo(Avatar.class);
	}

	@Test
	@DisplayName("유효하지 않은 이메일로 사용자를 생성할 수 없다")
	void rejectsInvalidEmail() {
		assertThatThrownBy(() -> new User("invalid-email", "password", "nickname"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("유효하지 않은 닉네임으로 사용자를 생성할 수 없다")
	void rejectsInvalidNickname() {
		assertThatThrownBy(() -> new User("user@example.com", "password", " "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("유효하지 않은 닉네임으로 변경할 수 없다")
	void rejectsInvalidNicknameChange() {
		User user = new User("user@example.com", "password", "nickname");

		assertThatThrownBy(() -> user.changeNickname(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("회원 탈퇴 시 탈퇴 처리 시각을 기록한다")
	void recordsWithdrawnAtWhenUserWithdraws() {
		User user = new User("user@example.com", "password", "nickname");

		user.withdraw();

		assertThat(user.getDeletedAt()).isNotNull();
		assertThat(user.isWithdrawn()).isTrue();
	}

	@Test
	@DisplayName("캡슐화된 행위로 닉네임과 아바타를 변경한다")
	void changesProfileThroughAggregateBehavior() {
		User user = new User("user@example.com", "password", "nickname", "old-avatar");

		user.changeNickname("new-nickname");
		user.changeAvatar("new-avatar");

		assertThat(user.getNickname()).isEqualTo("new-nickname");
		assertThat(user.getAvatar()).isEqualTo("new-avatar");
	}

	@Test
	@DisplayName("캡슐화된 행위로 보호된 비밀번호를 변경한다")
	void changesProtectedPasswordThroughAggregateBehavior() {
		User user = new User("user@example.com", "old-password", "nickname");

		user.updatePassword("new-password");

		assertThat(user.getPassword()).isEqualTo("new-password");
	}
}
