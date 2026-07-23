package com.pikume.back.creative.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreativeImageObjectKeyPolicy")
class CreativeImageObjectKeyPolicyTest {

	private final CreativeImageObjectKeyPolicy policy = new CreativeImageObjectKeyPolicy();

	@Test
	@DisplayName("기존 Diary AI 이미지 형식과 호환되는 private Object Key를 생성한다")
	void createsCompatiblePrivateObjectKey() {
		String objectKey = policy.createGeneratedImageObjectKey("PNG");

		assertThat(objectKey)
				.matches("private/diary-images/ai/[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{32}\\.png");
		assertThat(policy.contentType("PNG")).isEqualTo("image/png");
	}
}
