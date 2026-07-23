package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminStatisticsUseCase;
import com.pikume.back.admin.application.port.in.RecordAdminSecurityEventUseCase;
import com.pikume.back.admin.application.service.AdminDailyStatisticsResult;
import com.pikume.back.admin.application.service.AdminStatisticsResponse;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.principal.AdminPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsController")
class AdminStatisticsControllerTest {

	@Mock
	private AdminStatisticsUseCase adminStatisticsUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AdminStatisticsController controller = new AdminStatisticsController(adminStatisticsUseCase);
			mockMvc = MockMvcBuilders.standaloneSetup(controller)
					.setControllerAdvice(new AdminExceptionHandler(
							new ProblemDetailFactory(), mock(RecordAdminSecurityEventUseCase.class)))
					.setMessageConverters(
							new StringHttpMessageConverter(StandardCharsets.UTF_8),
							new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
									.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
									.build()))
					.setCustomArgumentResolvers(new AdminPrincipalResolver(new AdminPrincipal(
							"admin-1",
							AdminRole.OPERATOR.name(),
							"session-1")))
					.build();
	}

	@Test
	@DisplayName("GET /api/admin/statistics/dashboard는 통계 응답을 반환한다")
	void dashboardReturnsStatistics() throws Exception {
		LocalDate startDate = LocalDate.of(2026, 6, 11);
		LocalDate endDate = LocalDate.of(2026, 6, 17);
		given(adminStatisticsUseCase.getStatistics("admin-1", startDate, endDate))
				.willReturn(new AdminStatisticsResponse(
						startDate,
						endDate,
						42L,
						List.of(new AdminDailyStatisticsResult(startDate, 1, 2, 3, 4, 5, 6, 7, 8)),
						List.of(),
						List.of()));

		mockMvc.perform(get("/api/admin/statistics/dashboard")
						.param("startDate", "2026-06-11")
						.param("endDate", "2026-06-17"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentMemberCount").value(42))
				.andExpect(jsonPath("$.dailyStatistics[0].date").value("2026-06-11"))
				.andExpect(jsonPath("$.dailyStatistics[0].aiPhotoFailures").value(7));
	}

	@Test
	@DisplayName("GET /api/admin/statistics/dashboard.csv는 CSV 응답을 반환한다")
	void dashboardCsvReturnsCsv() throws Exception {
		LocalDate startDate = LocalDate.of(2026, 6, 11);
		LocalDate endDate = LocalDate.of(2026, 6, 17);
		given(adminStatisticsUseCase.getStatisticsCsv("admin-1", startDate, endDate))
				.willReturn("date,current_member_count\n2026-06-11,42\n");

		mockMvc.perform(get("/api/admin/statistics/dashboard.csv")
						.param("startDate", "2026-06-11")
						.param("endDate", "2026-06-17"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("admin-statistics.csv")))
				.andExpect(content().string(containsString("2026-06-11,42")));
	}

	private record AdminPrincipalResolver(AdminPrincipal adminUserDetails) implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AdminPrincipal.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return adminUserDetails;
		}
	}
}
