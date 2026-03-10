package com.pikume.back.support.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.support.application.port.in.InquiryUseCase;
import com.pikume.back.support.application.port.out.LoadUserInfoForSupportPort;
import com.pikume.back.support.application.port.out.SaveInquiryPort;
import com.pikume.back.support.application.port.out.SendFeedbackEmailPort;
import com.pikume.back.support.application.port.out.UploadInquiryImagePort;
import com.pikume.back.support.domain.Inquiry;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService implements InquiryUseCase {

	private final LoadUserInfoForSupportPort loadUserInfoForSupportPort;
	private final UploadInquiryImagePort uploadInquiryImagePort;
	private final SendFeedbackEmailPort sendFeedbackEmailPort;
	private final SaveInquiryPort saveInquiryPort;

	@Override
	public void submitInquiry(String userId, String content, UploadedFileData image) {
		if (!loadUserInfoForSupportPort.existsById(userId)) {
			throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
		}

		String imageUrl = null;
		if (image != null && !image.isEmpty()) {
			imageUrl = uploadInquiryImagePort.upload(image, userId);
		}

		try {
			sendFeedbackEmailPort.sendFeedbackEmail(content, image);
		} catch (Exception e) {
			log.error("피드백 이메일 전송 실패: {}", e.getMessage());
		}

		Inquiry inquiry = new Inquiry(userId, content, imageUrl);
		saveInquiryPort.save(inquiry);
		log.info("사용자 {}님의 문의 저장 완료", userId);
	}
}
