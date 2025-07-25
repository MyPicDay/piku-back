package store.piku.back.inquiry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.inquiry.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
