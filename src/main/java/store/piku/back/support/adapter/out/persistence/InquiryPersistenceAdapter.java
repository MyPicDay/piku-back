package store.piku.back.support.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.support.application.port.out.SaveInquiryPort;
import store.piku.back.support.domain.Inquiry;

@Component
@RequiredArgsConstructor
public class InquiryPersistenceAdapter implements SaveInquiryPort {

	private final InquiryJpaRepository inquiryJpaRepository;

	@Override
	public Inquiry save(Inquiry inquiry) {
		return inquiryJpaRepository.save(inquiry);
	}
}
