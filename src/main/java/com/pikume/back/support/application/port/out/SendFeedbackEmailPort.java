package com.pikume.back.support.application.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface SendFeedbackEmailPort {

	void sendFeedbackEmail(String content, MultipartFile image);
}
