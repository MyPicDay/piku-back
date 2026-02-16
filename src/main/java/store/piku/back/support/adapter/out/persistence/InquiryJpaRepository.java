package store.piku.back.support.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.support.domain.Inquiry;

public interface InquiryJpaRepository extends JpaRepository<Inquiry, Long> {
}
