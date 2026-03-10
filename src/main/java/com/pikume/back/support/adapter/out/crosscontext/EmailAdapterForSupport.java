package com.pikume.back.support.adapter.out.crosscontext;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.support.application.port.out.SendFeedbackEmailPort;
import com.pikume.back.user.auth.constants.EmailConstants;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EmailAdapterForSupport implements SendFeedbackEmailPort {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String adminEmail;

	@Override
	public void sendFeedbackEmail(String content, UploadedFileData image) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

			String subject = "[PikU] 피드백";
			String htmlContent = String.format(EmailConstants.FEEDBACK, content);

			helper.setFrom(adminEmail, "PikU | 피쿠");
			helper.setTo(adminEmail);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);

			if (image != null && !image.isEmpty()) {
				helper.addAttachment(
						Objects.requireNonNull(image.originalFilename()),
						new ByteArrayResource(image.bytes()));
			}

			mailSender.send(mimeMessage);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new RuntimeException("이메일 전송 중 오류 발생", e);
		}
	}
}
