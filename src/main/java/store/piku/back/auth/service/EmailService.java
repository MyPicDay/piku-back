package store.piku.back.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import store.piku.back.auth.repository.AllowedEmailDomainRepository;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AllowedEmailDomainRepository allowedEmailDomainRepository;

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

        String htmlContent = "<html><body>"
                + "<h2>이메일 인증</h2>"
                + "<p style='margin:10px 0;'>안녕하세요. 나만의 캐릭터로 기록하는 하루 한 장, <b>PikU</b> 입니다.</p>"
                + "<p>본인 인증을 위한 이메일 인증 코드는 다음과 같습니다.</p>"
                + "<p style='font-size: 20px; font-weight: bold; color: #1a73e8; margin: 15px 0; letter-spacing: 2px;'>" + code + "</p>"
                + "<p>이 코드를 웹사이트에 입력하여 인증을 완료해주세요.</p>"
                + "<br>"
                + "</body></html>";

        helper.setFrom("mypikuofficial@gmail.com", "PikU | 피쿠");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);

        return code;
    }

    public boolean isEmailAllowed(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        return allowedEmailDomainRepository.existsByDomain(domain);
    }
}