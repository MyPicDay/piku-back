package com.pikume.back.notification.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.exception.UserErrorCode;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("UserAdapterForNotification")
class UserAdapterForNotificationTest {

	private final LoadUserPort loadUserPort = mock(LoadUserPort.class);
	private final ImagePathToUrlConverter imagePathToUrlConverter = mock(ImagePathToUrlConverter.class);
	private final UserAdapterForNotification adapter = new UserAdapterForNotification(loadUserPort, imagePathToUrlConverter);

	@Test
	@DisplayName("사용자가 존재하면 닉네임을 반환한다")
	void getUserNickname() {
		given(loadUserPort.findById("user-1"))
				.willReturn(Optional.of(new User("user-1", "a@a.com", "pw", "피쿠", "avatar")));

		assertThat(adapter.getUserNickname("user-1")).isEqualTo("피쿠");
	}

	@Test
	@DisplayName("사용자가 없으면 UserNotFoundException을 던진다")
	void missingUserThrowsUserNotFoundException() {
		given(loadUserPort.findById("missing")).willReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.getUserNickname("missing"))
				.isInstanceOfSatisfying(UserNotFoundException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
	}
}
