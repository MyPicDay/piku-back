package com.pikume.back.support.adapter.out.email;

import com.pikume.back.support.application.dto.InquiryAttachment;
import com.pikume.back.support.application.port.out.SendInquiryNotificationPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InquiryEmailAdapter implements SendInquiryNotificationPort {

	private final JavaMailSender mailSender;
	private final InquiryEmailTemplate emailTemplate;

	@Value("${spring.mail.username}")
	private String adminEmail;

	@Override
	public void sendInquiryNotification(String content, InquiryAttachment attachment) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

			helper.setFrom(adminEmail, "PikU | 피쿠");
			helper.setTo(adminEmail);
			helper.setSubject(emailTemplate.subject());
			helper.setText(emailTemplate.render(content), true);

			if (attachment != null && !attachment.isEmpty()) {
				helper.addAttachment(
						Objects.requireNonNull(attachment.originalFilename()),
						new ByteArrayResource(attachment.bytes()));
			}

			mailSender.send(mimeMessage);
		} catch (MessagingException | UnsupportedEncodingException exception) {
			throw new IllegalStateException("문의 알림 메일을 전송할 수 없습니다.", exception);
		}
	}
}
