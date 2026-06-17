package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdminDailyStatisticsJpaRepository extends JpaRepository<AdminDailyStatistics, LocalDate> {

	List<AdminDailyStatistics> findByMetricDateBetween(LocalDate startDate, LocalDate endDate);
}
