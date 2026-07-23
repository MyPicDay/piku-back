package com.pikume.back.support.application.port.out;

import com.pikume.back.support.application.dto.InquiryAttachment;

public interface SendInquiryNotificationPort {

	void sendInquiryNotification(String content, InquiryAttachment attachment);
}
