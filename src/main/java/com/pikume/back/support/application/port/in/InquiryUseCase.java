package com.pikume.back.support.application.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface InquiryUseCase {

	void submitInquiry(String userId, String content, MultipartFile image);
}
