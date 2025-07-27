package store.piku.back.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.util.StringUtils;
import store.piku.back.auth.entity.AllowedEmail;
import store.piku.back.auth.repository.AllowedEmailDomainRepository;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.auth.constants.EmailConstants;

import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AllowedEmailDomainRepository allowedEmailDomainRepository;

    @Value("${spring.mail.username}")
    private String adminEmail;

    /**
     * 6자리 인증 코드를 생성합니다.
     * @return 생성된 6자리 숫자 문자열
     */
    private String createVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 인증 이메일을 발송하고, 생성된 인증 코드를 반환합니다.
     * @param toEmail 수신자 이메일 주소
     * @return 생성된 인증 코드
     */
    public String sendVerificationEmail(String toEmail) throws MessagingException, UnsupportedEncodingException {
        String code = createVerificationCode();
        String subject = "[PikU] 이메일 인증";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlContent = String.format(EmailConstants.AUTH_CODE_CONTENT, code);

        helper.setFrom(adminEmail, "PikU | 피쿠");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);

        return code;
    }

    public boolean isEmailAllowed(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1);
        return allowedEmailDomainRepository.existsByDomain(domain);
    }

    public List<String> getAllowedEmailDomains() {
        return allowedEmailDomainRepository.findAll().stream()
                .map(AllowedEmail::getDomain)
                .toList();
    public void sendFeedbackEmail(String content, MultipartFile image) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

        String subject = "[PikU] 피드백";
        String htmlContent = String.format(EmailConstants.FEEDBACK, content);

        helper.setFrom(adminEmail, "PikU | 피쿠");
        helper.setTo(adminEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        if (image != null && !image.isEmpty()) {
            helper.addAttachment(Objects.requireNonNull(image.getOriginalFilename()), image);
        }

        mailSender.send(mimeMessage);
    }
}