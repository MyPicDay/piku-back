package com.pikume.back.support.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InquiryAttachment")
class InquiryAttachmentTest {

	@Test
	@DisplayName("첨부 바이트를 생성과 조회 시 방어 복사한다")
	void defensivelyCopiesBytes() {
		byte[] source = "image".getBytes();
		InquiryAttachment attachment = new InquiryAttachment("image.png", "image/png", source);
		source[0] = 'X';

		byte[] firstRead = attachment.bytes();
		firstRead[0] = 'Y';

		assertThat(attachment.bytes()).containsExactly("image".getBytes());
	}
}
