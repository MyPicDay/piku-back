package com.pikume.back.admin.adapter.out.crosscontext;

import com.pikume.back.admin.application.service.AdminDailyCount;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.user.application.port.out.LoadUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsSourceAdapter")
class AdminStatisticsSourceAdapterTest {

	@Mock
	private LoadUserPort loadUserPort;
	@Mock
	private LoadDiaryPort loadDiaryPort;
	@Mock
	private LoadGenerationPort loadGenerationPort;

	@Test
	@DisplayName("현재 회원 수는 user 도메인 포트로 조회한다")
	void countCurrentMembersDelegatesToUserPort() {
		given(loadUserPort.countActiveMembers()).willReturn(42L);

		long result = adapter().countCurrentMembers();

		assertThat(result).isEqualTo(42L);
	}

	@Test
	@DisplayName("일별 가입 회원 수는 user 도메인 결과를 관리자 통계 결과로 변환한다")
	void countSignupMembersByDateMapsUserCounts() {
		LocalDate startDate = LocalDate.of(2026, 6, 10);
		LocalDate endDate = LocalDate.of(2026, 6, 11);
		given(loadUserPort.countSignupMembersByDate(startDate, endDate))
				.willReturn(List.of(new LoadUserPort.DailyCount(startDate, 3L)));

		List<AdminDailyCount> result = adapter().countSignupMembersByDate(startDate, endDate);

		assertThat(result).containsExactly(new AdminDailyCount(startDate, 3L));
	}

	@Test
	@DisplayName("일별 일기 생성 수는 diary 도메인 결과를 관리자 통계 결과로 변환한다")
	void countDiaryCreationsByDateMapsDiaryCounts() {
		LocalDate startDate = LocalDate.of(2026, 6, 10);
		LocalDate endDate = LocalDate.of(2026, 6, 11);
		given(loadDiaryPort.countCreatedDiariesByDate(startDate, endDate))
				.willReturn(List.of(new LoadDiaryPort.DailyCount(endDate, 5L)));

		List<AdminDailyCount> result = adapter().countDiaryCreationsByDate(startDate, endDate);

		assertThat(result).containsExactly(new AdminDailyCount(endDate, 5L));
	}

	@Test
	@DisplayName("일별 AI 사진 성공 수는 creative 도메인 결과를 관리자 통계 결과로 변환한다")
	void countAiPhotoSuccessesByDateMapsCreativeCounts() {
		LocalDate startDate = LocalDate.of(2026, 6, 10);
		LocalDate endDate = LocalDate.of(2026, 6, 11);
		given(loadGenerationPort.countSuccessfulGenerationsByDate(startDate, endDate))
				.willReturn(List.of(new LoadGenerationPort.DailyCount(endDate, 7L)));

		List<AdminDailyCount> result = adapter().countAiPhotoSuccessesByDate(startDate, endDate);

		assertThat(result).containsExactly(new AdminDailyCount(endDate, 7L));
	}

	private AdminStatisticsSourceAdapter adapter() {
		return new AdminStatisticsSourceAdapter(loadUserPort, loadDiaryPort, loadGenerationPort);
	}
}
