package com.pikume.back.support.application.port.out;

import com.pikume.back.global.dto.UploadedFileData;

public interface SendFeedbackEmailPort {

	void sendFeedbackEmail(String content, UploadedFileData image);
}
