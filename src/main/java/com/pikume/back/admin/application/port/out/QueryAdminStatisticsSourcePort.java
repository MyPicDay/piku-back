package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.application.dto.AdminDailyCount;

import java.time.LocalDate;
import java.util.List;

public interface QueryAdminStatisticsSourcePort {

	long countCurrentMembers();

	List<AdminDailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countAiPhotoSuccessesByDate(LocalDate startDate, LocalDate endDate);
}
