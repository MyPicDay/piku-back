package com.pikume.back.security.adapter.in.web;

import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.auth.application.dto.LoginResult;
import com.pikume.back.security.adapter.in.web.dto.response.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserResponseMapper")
class AuthUserResponseMapperTest {

	@InjectMocks
	private AuthUserResponseMapper authUserResponseMapper;

	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;

	@Test
	@DisplayName("로그인 사용자 정보의 avatar path를 display URL로 변환한다")
	void convertsLoginUserAvatarPathToDisplayUrl() {
		LoginResult.UserInfo rawUserInfo = new LoginResult.UserInfo(
				"user-1",
				"pikume",
				"public/characters/fixed/base_image_1.webp");
		given(imagePathToUrlConverter.userAvatarImageUrl("public/characters/fixed/base_image_1.webp"))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		UserInfo result = authUserResponseMapper.toDisplayUserInfo(rawUserInfo);

		assertThat(result.getAvatarUrl())
				.isEqualTo("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("인증 principal의 avatar path를 display URL로 변환한다")
	void convertsCurrentUserPrincipalAvatarPathToDisplayUrl() {
		UserPrincipal userDetails = UserPrincipal.withAvatarPath(
				"user-1",
				"pikume",
				"public/characters/fixed/base_image_1.webp");
		given(imagePathToUrlConverter.userAvatarImageUrl("public/characters/fixed/base_image_1.webp"))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		UserInfo result = authUserResponseMapper.toDisplayUserInfo(userDetails);

		assertThat(result.getAvatarUrl())
				.isEqualTo("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
	}
}
