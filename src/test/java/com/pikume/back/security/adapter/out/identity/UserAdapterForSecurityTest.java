package com.pikume.back.security.adapter.out.identity;

import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
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
@DisplayName("UserAdapterForSecurity")
class UserAdapterForSecurityTest {

	@InjectMocks
	private UserAdapterForSecurity userAdapterForSecurity;

	@Mock
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("인증 사용자 조회 시 DB의 avatar object key를 raw path로 반환한다")
	void returnsRawAvatarObjectKey() {
		User user = new User(
				"user@example.com",
				"encoded-password",
				"pikume",
				"public/characters/fixed/base_image_1.png");
		given(userJpaRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		var result = userAdapterForSecurity.findByEmail("user@example.com");

		assertThat(result).isPresent();
		assertThat(result.get().avatarPath())
				.isEqualTo("public/characters/fixed/base_image_1.png");
	}
}
