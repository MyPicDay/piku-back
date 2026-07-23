package com.pikume.back.support.application.port.in;

import com.pikume.back.support.application.dto.SubmitInquiryCommand;

public interface SubmitInquiryUseCase {

	void submitInquiry(SubmitInquiryCommand command);
}
