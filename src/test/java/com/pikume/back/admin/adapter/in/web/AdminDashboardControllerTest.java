package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminDashboardUseCase;
import com.pikume.back.admin.application.port.in.RecordAdminSecurityEventUseCase;
import com.pikume.back.admin.application.service.AdminDashboardResponse;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.config.AdminUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardController")
class AdminDashboardControllerTest {

	@Mock
	private AdminDashboardUseCase adminDashboardUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminDashboardUseCase))
				.setControllerAdvice(new AdminExceptionHandler(
						new ProblemDetailFactory(), mock(RecordAdminSecurityEventUseCase.class)))
				.setMessageConverters(new MappingJackson2HttpMessageConverter(
						Jackson2ObjectMapperBuilder.json()
								.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
								.build()))
				.setCustomArgumentResolvers(new AdminPrincipalResolver(new AdminUserDetails(
						"admin-1",
						AdminRole.VIEWER.name(),
						"session-1")))
				.build();
	}

	@Test
	@DisplayName("GET /api/admin/dashboard는 인증 관리자에게 통합 대시보드를 반환한다")
	void dashboardReturnsIntegratedDashboard() throws Exception {
		LocalDate date = LocalDate.of(2026, 6, 22);
		given(adminDashboardUseCase.getDashboard("admin-1")).willReturn(new AdminDashboardResponse(
				new AdminDashboardResponse.KeyMetrics(100, 90, 30, 25, 50, 40, 200, 180),
				List.of(new AdminDashboardResponse.DailyActiveUser(date, 7)),
				new AdminDashboardResponse.AiPhotoGeneration(5, 2),
				List.of(new AdminDashboardResponse.WeeklyActivity(
						date, date.plusDays(6), 3, 6)),
				List.of(new AdminDashboardResponse.DailySummary(date, 3, 7, 6, 8))));

		mockMvc.perform(get("/api/admin/dashboard"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(jsonPath("$.keyMetrics.currentCumulativeMemberCount").value(100))
				.andExpect(jsonPath("$.keyMetrics.cumulativeMemberCountSevenDaysAgo").value(90))
				.andExpect(jsonPath("$.dailyActiveUsers[0].date").value("2026-06-22"))
				.andExpect(jsonPath("$.dailyActiveUsers[0].dau").value(7))
				.andExpect(jsonPath("$.aiPhotoGeneration.successCount").value(5))
				.andExpect(jsonPath("$.weeklyActivity[0].periodEndDate").value("2026-06-28"))
				.andExpect(jsonPath("$.dailySummary[0].aiPhotoRequestCount").value(8));

		then(adminDashboardUseCase).should().getDashboard("admin-1");
	}

	@Test
	@DisplayName("인증 관리자가 없으면 Problem Details 401을 반환한다")
	void dashboardRequiresAuthenticatedAdmin() throws Exception {
		MockMvc unauthenticatedMockMvc = MockMvcBuilders
				.standaloneSetup(new AdminDashboardController(adminDashboardUseCase))
				.setControllerAdvice(new AdminExceptionHandler(
						new ProblemDetailFactory(), mock(RecordAdminSecurityEventUseCase.class)))
				.setCustomArgumentResolvers(new AdminPrincipalResolver(null))
				.build();

		unauthenticatedMockMvc.perform(get("/api/admin/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type")
						.value("https://api.pikume.com/problems/admin/unauthenticated"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("관리자 인증이 필요합니다."));
	}

	private record AdminPrincipalResolver(AdminUserDetails adminUserDetails) implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AdminUserDetails.class);
		}

		@Override
		public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				WebDataBinderFactory binderFactory) {
			return adminUserDetails;
		}
	}
}
