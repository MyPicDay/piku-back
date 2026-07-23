package com.pikume.back.support.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InquiryAttachmentObjectKeyPolicy")
class InquiryAttachmentObjectKeyPolicyTest {

	private final InquiryAttachmentObjectKeyPolicy policy = new InquiryAttachmentObjectKeyPolicy();

	@Test
	@DisplayName("정상 UUID 사용자는 기존 날짜와 앞 8자 Object Key 형식을 유지한다")
	void keepsExistingUuidObjectKeyFormat() {
		String objectKey = policy.createObjectKey(
				"550e8400-e29b-41d4-a716-446655440000",
				"inquiry.png",
				LocalDate.of(2026, 7, 23));

		assertThat(objectKey)
				.matches("inquiry/2026-07-23/550e8400_[0-9a-f-]{36}\\.png");
	}

	@Test
	@DisplayName("짧은 사용자 식별자와 경로 문자가 포함된 확장자를 안전하게 처리한다")
	void handlesShortUserAndUnsafeExtension() {
		String objectKey = policy.createObjectKey(
				"u/1",
				"image.jpg/../../secret",
				LocalDate.of(2026, 7, 23));

		assertThat(objectKey).startsWith("inquiry/2026-07-23/u_1_");
		assertThat(objectKey).doesNotContain("..", "secret");
	}
}
