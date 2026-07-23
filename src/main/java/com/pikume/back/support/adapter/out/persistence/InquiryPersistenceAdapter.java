package com.pikume.back.support.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.support.application.port.out.RecordInquiryPort;
import com.pikume.back.support.domain.Inquiry;

@Component
@RequiredArgsConstructor
public class InquiryPersistenceAdapter implements RecordInquiryPort {

	private final InquiryJpaRepository inquiryJpaRepository;

	@Override
	public Inquiry recordInquiry(Inquiry inquiry) {
		return inquiryJpaRepository.save(inquiry);
	}
}
