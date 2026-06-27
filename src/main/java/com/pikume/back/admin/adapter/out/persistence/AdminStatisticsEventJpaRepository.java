package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminStatisticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminStatisticsEventJpaRepository extends JpaRepository<AdminStatisticsEvent, Long> {
}
