package com.pikume.back.support.application.port.in;

import com.pikume.back.global.dto.UploadedFileData;

public interface InquiryUseCase {

	void submitInquiry(String userId, String content, UploadedFileData image);
}
