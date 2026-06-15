package com.pikume.back.global.util;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImagePathToUrlConverter")
class ImagePathToUrlConverterTest {

	@InjectMocks
	private ImagePathToUrlConverter converter;

	@Mock
	private ResolveImageUrlPort resolveImageUrlPort;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "api.example.com", 443, "api.example.com", "https://api.example.com", "TestAgent", "127.0.0.1");

	@Test
	@DisplayName("canonical fixed character object key는 storage public URL로 변환한다")
	void convertsCanonicalFixedCharacterObjectKeyToPublicStorageUrl() {
		given(resolveImageUrlPort.getPhotoUrl("public/characters/fixed/base_image_1.webp", true))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		String result = converter.userAvatarImageUrl("public/characters/fixed/base_image_1.webp", requestMetaInfo);

		assertThat(result).isEqualTo("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("fixed character 파일명은 canonical object key로 정규화한 뒤 public URL로 변환한다")
	void convertsFixedCharacterFileNameToPublicStorageUrl() {
		given(resolveImageUrlPort.getPhotoUrl("public/characters/fixed/base_image_1.webp", true))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		String result = converter.fixedCharacterImageUrl("base_image_1.webp", requestMetaInfo);

		assertThat(result).isEqualTo("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("legacy fixed character avatar path는 canonical object key로 정규화한 뒤 public URL로 변환한다")
	void convertsLegacyFixedCharacterAvatarPathToPublicStorageUrl() {
		given(resolveImageUrlPort.getPhotoUrl("public/characters/fixed/base_image_1.webp", true))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		String result = converter.userAvatarImageUrl("characters/fixed/base_image_1.webp", requestMetaInfo);

		assertThat(result).isEqualTo("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("absolute avatar URL 입력값은 그대로 반환한다")
	void returnsAbsoluteAvatarUrlAsIs() {
		String result = converter.userAvatarImageUrl("https://cdn.example.com/avatar.png", requestMetaInfo);

		assertThat(result).isEqualTo("https://cdn.example.com/avatar.png");
		then(resolveImageUrlPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("blank avatar path는 빈 문자열을 반환한다")
	void returnsEmptyStringForBlankAvatarPath() {
		String result = converter.userAvatarImageUrl(" ", requestMetaInfo);

		assertThat(result).isEmpty();
		then(resolveImageUrlPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("유효하지 않은 fixed character path는 빈 문자열을 반환한다")
	void returnsEmptyStringForInvalidFixedCharacterPath() {
		String result = converter.fixedCharacterImageUrl("group/../bad.png", requestMetaInfo);

		assertThat(result).isEmpty();
		then(resolveImageUrlPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("유효하지 않은 user avatar path는 빈 문자열을 반환한다")
	void returnsEmptyStringForInvalidUserAvatarPath() {
		String result = converter.userAvatarImageUrl("characters/fixed/group/../bad.png", requestMetaInfo);

		assertThat(result).isEmpty();
		then(resolveImageUrlPort).shouldHaveNoInteractions();
	}
}
