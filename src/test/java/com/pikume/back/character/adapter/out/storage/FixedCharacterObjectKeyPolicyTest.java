package com.pikume.back.character.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FixedCharacterObjectKeyPolicy")
class FixedCharacterObjectKeyPolicyTest {

	private final FixedCharacterObjectKeyPolicy policy = new FixedCharacterObjectKeyPolicy();

	@Test
	@DisplayName("파일명과 legacy 경로를 canonical fixed character object key로 변환한다")
	void canonicalizesCompatibleReferences() {
		assertThat(policy.canonicalizeFixedCharacterObjectKey("base.webp"))
				.isEqualTo("public/characters/fixed/base.webp");
		assertThat(policy.canonicalizeFixedCharacterObjectKey("characters/fixed/base.webp"))
				.isEqualTo("public/characters/fixed/base.webp");
		assertThat(policy.canonicalizeFixedCharacterObjectKey("public/characters/fixed/base.webp"))
				.isEqualTo("public/characters/fixed/base.webp");
	}

	@Test
	@DisplayName("절대 URL 호환 참조는 변경하지 않는다")
	void preservesAbsoluteUrlCompatibility() {
		assertThat(policy.canonicalizeFixedCharacterObjectKey("https://assets.example.com/base.webp"))
				.isEqualTo("https://assets.example.com/base.webp");
	}

	@Test
	@DisplayName("경로 이탈과 역슬래시를 거부한다")
	void rejectsUnsafeReferences() {
		assertThatThrownBy(() -> policy.canonicalizeFixedCharacterObjectKey("group/../base.webp"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> policy.canonicalizeFixedCharacterObjectKey("group\\base.webp"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
