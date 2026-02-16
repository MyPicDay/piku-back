package store.piku.back.user.auth.adapter.out.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import store.piku.back.user.auth.application.port.out.SendVerificationEmailPort;
import store.piku.back.user.auth.domain.AllowedEmail;
import store.piku.back.user.auth.adapter.out.persistence.AllowedEmailDomainJpaRepository;
import store.piku.back.user.auth.constants.EmailConstants;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class SmtpEmailAdapter implements SendVerificationEmailPort {

	private final JavaMailSender mailSender;
	private final AllowedEmailDomainJpaRepository allowedEmailDomainRepository;

	@Value("${spring.mail.username}")
	private String adminEmail;

	@Override
	public String sendVerificationEmail(String email) {
		String code = createVerificationCode();
		String subject = "[PikU] 이메일 인증";

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

			String htmlContent = String.format(EmailConstants.AUTH_CODE_CONTENT, code);

			helper.setFrom(adminEmail, "PikU | 피쿠");
			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new RuntimeException("이메일 발송에 실패했습니다.", e);
		}

		return code;
	}

	@Override
	public boolean isEmailAllowed(String email) {
		if (!StringUtils.hasText(email) || !email.contains("@")) {
			return false;
		}
		String domain = email.substring(email.indexOf("@") + 1);
		return allowedEmailDomainRepository.existsByDomain(domain);
	}

	@Override
	public List<String> getAllowedEmailDomains() {
		return allowedEmailDomainRepository.findAll().stream()
				.map(AllowedEmail::getDomain)
				.toList();
	}

	private String createVerificationCode() {
		Random random = new Random();
		int code = 100000 + random.nextInt(900000);
		return String.valueOf(code);
	}
}
