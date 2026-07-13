package com.pikume.back.notification.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.exception.UserErrorCode;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("UserAdapterForNotification")
class UserAdapterForNotificationTest {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase = mock(QueryUserReferenceUseCase.class);
	private final ImagePathToUrlConverter imagePathToUrlConverter = mock(ImagePathToUrlConverter.class);
	private final UserAdapterForNotification adapter = new UserAdapterForNotification(
			queryUserReferenceUseCase, imagePathToUrlConverter);

	@Test
	@DisplayName("사용자가 존재하면 닉네임을 반환한다")
	void getUserNickname() {
		given(queryUserReferenceUseCase.getUserReference("user-1"))
				.willReturn(new UserReferenceView("user-1", "피쿠", "avatar"));

		assertThat(adapter.getUserNickname("user-1")).isEqualTo("피쿠");
	}

	@Test
	@DisplayName("사용자가 없으면 UserNotFoundException을 던진다")
	void missingUserThrowsUserNotFoundException() {
		given(queryUserReferenceUseCase.getUserReference("missing")).willThrow(new UserNotFoundException());

		assertThatThrownBy(() -> adapter.getUserNickname("missing"))
				.isInstanceOfSatisfying(UserNotFoundException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
	}
}
