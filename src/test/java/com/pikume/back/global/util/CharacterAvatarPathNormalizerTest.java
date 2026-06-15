package com.pikume.back.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CharacterAvatarPathNormalizer")
class CharacterAvatarPathNormalizerTest {

	@Test
	@DisplayName("fixed character 그룹 경로는 canonical object key로 유지한다")
	void normalizesGroupedFixedCharacterObjectKey() {
		assertThat(CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(
				"characters/fixed/group/base_image_1.webp"))
				.isEqualTo("public/characters/fixed/group/base_image_1.webp");
		assertThat(CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(
				"public/characters/fixed/group/base_image_1.webp"))
				.isEqualTo("public/characters/fixed/group/base_image_1.webp");
	}

	@Test
	@DisplayName("fixed character 상대 경로의 traversal은 거부한다")
	void rejectsFixedCharacterPathTraversal() {
		assertThatThrownBy(() -> CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(
				"characters/fixed/../base_image_1.webp"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(
				"characters/fixed/group/../base_image_1.webp"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
