package com.pikume.back.user.application.service;

import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserReferenceQueryService")
class UserReferenceQueryServiceTest {

	@Mock
	private LoadUserAccountPort loadUserAccountPort;

	@Test
	@DisplayName("사용자 Aggregate를 외부 Context용 공개 참조 View로 변환한다")
	void mapsUserToPublicReferenceView() {
		User user = new User("user-1", "user@example.com", "password", "pikume", "avatar-key");
		given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));

		var result = new UserReferenceQueryService(loadUserAccountPort).findUserReference("user-1");

		assertThat(result).contains(new com.pikume.back.user.application.dto.UserReferenceView(
				"user-1", "pikume", "avatar-key"));
	}
}
