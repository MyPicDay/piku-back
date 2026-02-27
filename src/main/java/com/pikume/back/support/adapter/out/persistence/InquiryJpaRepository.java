package com.pikume.back.support.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.support.domain.Inquiry;

public interface InquiryJpaRepository extends JpaRepository<Inquiry, Long> {
}
