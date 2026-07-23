package com.pikume.back.support.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inquiry")
class InquiryTest {

	@Test
	@DisplayName("제출자, 내용과 선택적 첨부 저장 참조로 문의를 만든다")
	void submitsInquiry() {
		Inquiry inquiry = Inquiry.submit("user-1", "문의 내용", "inquiry/path/image.png");

		assertThat(inquiry.getUserId()).isEqualTo("user-1");
		assertThat(inquiry.getContent()).isEqualTo("문의 내용");
		assertThat(inquiry.getAttachmentReference()).isEqualTo("inquiry/path/image.png");
	}

	@Test
	@DisplayName("현재 계약대로 공백 내용은 Domain에서 새로 거부하지 않는다")
	void preservesBlankContentPolicy() {
		assertThat(Inquiry.submit("user-1", " ", null).getContent()).isEqualTo(" ");
	}

	@Test
	@DisplayName("DB 저장 제약인 null 내용과 1000자 초과 내용은 거부한다")
	void rejectsContentOutsideCurrentStorageConstraint() {
		assertThatThrownBy(() -> Inquiry.submit("user-1", null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Inquiry.submit("user-1", "a".repeat(1001), null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
