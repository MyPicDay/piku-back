package com.pikume.back.user.auth.adapter.out.email;

import com.pikume.back.user.auth.application.exception.AuthErrorCode;
import com.pikume.back.user.auth.application.exception.AuthException;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("SmtpEmailAdapter")
class SmtpEmailAdapterTest {

	@Test
	@DisplayName("Spring Mail 발송 실패를 Application 오류 의미로 번역한다")
	void translatesMailSendFailure() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		given(mailSender.createMimeMessage()).willReturn(message);
		willThrow(new MailSendException("smtp unavailable")).given(mailSender).send(message);
		SmtpEmailAdapter adapter = new SmtpEmailAdapter(mailSender);
		ReflectionTestUtils.setField(adapter, "adminEmail", "admin@example.com");

		assertThatThrownBy(() -> adapter.issueVerificationEmail("user@example.com"))
				.isInstanceOfSatisfying(AuthException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_SEND_FAILURE));
	}
}
