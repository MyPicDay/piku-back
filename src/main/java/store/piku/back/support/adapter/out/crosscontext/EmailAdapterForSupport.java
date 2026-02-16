package store.piku.back.support.adapter.out.crosscontext;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.auth.service.EmailService;
import store.piku.back.support.application.port.out.SendFeedbackEmailPort;

import java.io.UnsupportedEncodingException;

@Component
@RequiredArgsConstructor
public class EmailAdapterForSupport implements SendFeedbackEmailPort {

	private final EmailService emailService;

	@Override
	public void sendFeedbackEmail(String content, MultipartFile image) {
		try {
			emailService.sendFeedbackEmail(content, image);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new RuntimeException("이메일 전송 중 오류 발생", e);
		}
	}
}
