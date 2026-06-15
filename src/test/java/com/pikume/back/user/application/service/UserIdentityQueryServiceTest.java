package com.pikume.back.user.application.service;

import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserIdentityQueryService")
class UserIdentityQueryServiceTest {

	@InjectMocks
	private UserIdentityQueryService userIdentityQueryService;

	@Mock
	private LoadUserPort loadUserPort;

	@Test
	@DisplayName("사용자 ID로 인증에 필요한 사용자 식별 정보를 조회한다")
	void findByIdReturnsUserIdentity() {
		User user = new User(
				"user-id",
				"user@example.com",
				"encoded-password",
				"pikume",
				"public/characters/fixed/base_image_1.png");
		given(loadUserPort.findById("user-id")).willReturn(Optional.of(user));

		var result = userIdentityQueryService.findById("user-id");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().passwordHash()).isEqualTo("encoded-password");
		assertThat(result.get().avatarPath()).isEqualTo("public/characters/fixed/base_image_1.png");
	}

	@Test
	@DisplayName("이메일로 인증에 필요한 사용자 식별 정보를 조회한다")
	void findByEmailReturnsUserIdentity() {
		User user = new User(
				"user-id",
				"user@example.com",
				"encoded-password",
				"pikume",
				"public/characters/fixed/base_image_1.png");
		given(loadUserPort.findByEmail("user@example.com")).willReturn(Optional.of(user));

		var result = userIdentityQueryService.findByEmail("user@example.com");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().passwordHash()).isEqualTo("encoded-password");
		assertThat(result.get().nickname()).isEqualTo("pikume");
	}
}
