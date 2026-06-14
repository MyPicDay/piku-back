package com.pikume.back.security.adapter.out.crosscontext;

import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
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
@DisplayName("UserAdapterForSecurity")
class UserAdapterForSecurityTest {

	@InjectMocks
	private UserAdapterForSecurity userAdapterForSecurity;

	@Mock
	private QueryUserIdentityUseCase queryUserIdentityUseCase;

	@Test
	@DisplayName("인증 이메일로 사용자 identity를 조회해 인증 사용자 view로 변환한다")
	void findByEmailReturnsAuthUserView() {
		given(queryUserIdentityUseCase.findByEmail("user@example.com"))
				.willReturn(Optional.of(new UserIdentityView(
						"user-id",
						"user@example.com",
						"encoded-password",
						"pikume",
						"public/characters/fixed/base_image_1.png")));

		var result = userAdapterForSecurity.findByEmail("user@example.com");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().email()).isEqualTo("user@example.com");
		assertThat(result.get().password()).isEqualTo("encoded-password");
		assertThat(result.get().avatarPath())
				.isEqualTo("public/characters/fixed/base_image_1.png");
	}

	@Test
	@DisplayName("인증 사용자 ID로 사용자 identity를 조회해 인증 사용자 view로 변환한다")
	void findByIdReturnsAuthUserView() {
		given(queryUserIdentityUseCase.findById("user-id"))
				.willReturn(Optional.of(new UserIdentityView(
						"user-id",
						"user@example.com",
						"encoded-password",
						"pikume",
						"public/characters/fixed/base_image_1.png")));

		var result = userAdapterForSecurity.findById("user-id");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().email()).isEqualTo("user@example.com");
		assertThat(result.get().password()).isEqualTo("encoded-password");
		assertThat(result.get().nickname()).isEqualTo("pikume");
	}
}
