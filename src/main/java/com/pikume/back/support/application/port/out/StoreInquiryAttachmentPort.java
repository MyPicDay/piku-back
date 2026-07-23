package com.pikume.back.support.application.port.out;

import com.pikume.back.support.application.dto.InquiryAttachment;

public interface StoreInquiryAttachmentPort {

	String storeInquiryAttachment(InquiryAttachment attachment, String userId);
}
