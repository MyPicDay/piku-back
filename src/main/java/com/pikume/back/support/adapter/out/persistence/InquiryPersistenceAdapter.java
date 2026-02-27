package com.pikume.back.support.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.support.application.port.out.SaveInquiryPort;
import com.pikume.back.support.domain.Inquiry;

@Component
@RequiredArgsConstructor
public class InquiryPersistenceAdapter implements SaveInquiryPort {

	private final InquiryJpaRepository inquiryJpaRepository;

	@Override
	public Inquiry save(Inquiry inquiry) {
		return inquiryJpaRepository.save(inquiry);
	}
}
