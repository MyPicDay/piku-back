package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.application.service.AdminDailyCount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryAdminDashboardSourcePort {

	long countCurrentCumulativeMembers();

	long countCumulativeMembersBefore(LocalDateTime cutoffExclusive);

	long countAllSuccessfulAiPhotos();

	long countSuccessfulAiPhotosBefore(LocalDateTime cutoffExclusive);

	long countAllCreatedDiaries();

	long countCreatedDiariesBefore(LocalDateTime cutoffExclusive);

	List<AdminDailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countAllSuccessfulAiPhotosByDate(LocalDate startDate, LocalDate endDate);
}
