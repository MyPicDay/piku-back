package com.pikume.back.support.adapter.out.email;

import com.pikume.back.support.application.dto.InquiryAttachment;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("InquiryEmailAdapter")
class InquiryEmailAdapterTest {

	@Test
	@DisplayName("Support 소유 제목과 본문으로 첨부 포함 운영 메일을 전송한다")
	void sendsSupportOwnedInquiryEmail() throws Exception {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		given(mailSender.createMimeMessage()).willReturn(message);
		InquiryEmailAdapter adapter = adapter(mailSender);

		adapter.sendInquiryNotification(
				"문의 내용",
				new InquiryAttachment("inquiry.png", "image/png", "data".getBytes()));

		assertThat(message.getSubject()).isEqualTo("[PikU] 피드백");
		assertThat(message.getAllRecipients()[0].toString()).isEqualTo("admin@example.com");
		then(mailSender).should().send(message);
	}

	@Test
	@DisplayName("메일 Provider 실패는 호출자가 현재 실패 무시 정책을 적용할 수 있게 전파한다")
	void propagatesMailProviderFailure() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		given(mailSender.createMimeMessage()).willReturn(message);
		willThrow(new MailSendException("smtp secret")).given(mailSender).send(message);

		assertThatThrownBy(() -> adapter(mailSender)
				.sendInquiryNotification("문의 내용", null))
				.isInstanceOf(MailSendException.class);
	}

	private InquiryEmailAdapter adapter(JavaMailSender mailSender) {
		InquiryEmailAdapter adapter =
				new InquiryEmailAdapter(mailSender, new InquiryEmailTemplate());
		ReflectionTestUtils.setField(adapter, "adminEmail", "admin@example.com");
		return adapter;
	}
}
