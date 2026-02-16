package store.piku.back.support.application.port.out;

import store.piku.back.support.domain.Inquiry;

public interface SaveInquiryPort {

	Inquiry save(Inquiry inquiry);
}
