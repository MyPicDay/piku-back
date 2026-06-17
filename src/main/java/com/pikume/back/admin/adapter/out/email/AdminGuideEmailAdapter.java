package com.pikume.back.admin.adapter.out.email;

import com.pikume.back.admin.application.port.out.SendAdminGuideEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminGuideEmailAdapter implements SendAdminGuideEmailPort {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Value("${admin.ops-url:https://pikume-ops.pikume.com}")
	private String adminOpsUrl;

	@Override
	public void sendAccountCreatedGuide(String email, String temporaryLoginId, LocalDateTime temporaryCredentialExpiresAt) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

			String html = """
					<p>Pikume 관리자 계정이 생성되었습니다.</p>
					<p>관리자 페이지: %s</p>
					<p>임시 로그인 아이디: %s</p>
					<p>임시 자격 증명 만료 시각: %s</p>
					<p>임시 패스워드는 계정 발급자에게 별도 채널로 전달받아 주세요.</p>
					<p>이 이메일에는 임시 패스워드가 포함되지 않습니다.</p>
					""".formatted(adminOpsUrl, temporaryLoginId, temporaryCredentialExpiresAt);

			helper.setFrom(fromEmail, "Pikume Ops");
			helper.setTo(email);
			helper.setSubject("[Pikume Ops] 관리자 계정 생성 안내");
			helper.setText(html, true);

			mailSender.send(message);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new RuntimeException("관리자 안내 이메일 발송에 실패했습니다.", e);
		}
	}
}
