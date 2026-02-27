package com.pikume.back.support.application.port.out;

import com.pikume.back.support.domain.Inquiry;

public interface SaveInquiryPort {

	Inquiry save(Inquiry inquiry);
}
