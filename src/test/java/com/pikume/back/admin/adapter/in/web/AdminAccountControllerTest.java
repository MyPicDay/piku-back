package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.CreateAdminAccountRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminAccountOperationUseCase;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.service.AdminAccountSummaryResult;
import com.pikume.back.admin.application.service.AdminAccountDetailResult;
import com.pikume.back.admin.application.service.CreateAdminAccountCommand;
import com.pikume.back.admin.application.service.CreateAdminAccountResult;
import com.pikume.back.admin.domain.AdminAccountStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountController")
class AdminAccountControllerTest {

	@Mock
	private CreateAdminAccountUseCase createAdminAccountUseCase;
	@Mock
	private AdminAccountOperationUseCase adminAccountOperationUseCase;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		AdminAccountController controller = new AdminAccountController(createAdminAccountUseCase, adminAccountOperationUseCase);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new AdminExceptionHandler(
						new ProblemDetailFactory(), mock(AdminSessionTelemetryPort.class)))
				.setCustomArgumentResolvers(new AdminPrincipalResolver(new AdminUserDetails(
						"admin-1",
						AdminRole.SUPER_ADMIN.name(),
						"session-1")))
				.build();
	}

	@Test
	@DisplayName("GET /api/admin/accounts는 관리자 식별값과 마스킹된 식별자를 반환한다")
	void listReturnsAdminIdAndMaskedIdentifiers() throws Exception {
		given(adminAccountOperationUseCase.list("admin-1"))
				.willReturn(List.of(new AdminAccountSummaryResult(
						"target-admin",
						"vi***@pikume.com",
						"vi***ne",
						"조회자1",
						AdminRole.VIEWER,
						AdminAccountStatus.ACTIVE,
						false,
						true,
						null,
						LocalDateTime.of(2026, 6, 17, 13, 0))));

		mockMvc.perform(get("/api/admin/accounts")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].adminId").value("target-admin"))
				.andExpect(jsonPath("$[0].email").value("vi***@pikume.com"))
				.andExpect(jsonPath("$[0].loginId").value("vi***ne"));
	}

	@Test
	@DisplayName("GET /api/admin/accounts/{adminId}는 관리자 식별값으로 상세를 조회한다")
	void detailUsesAdminIdPath() throws Exception {
		given(adminAccountOperationUseCase.detailById("admin-1", "target-admin"))
				.willReturn(new AdminAccountDetailResult(
						"target-admin",
						"op***@pikume.com",
						"op***ne",
						"운영자1",
						AdminRole.OPERATOR,
						AdminAccountStatus.ACTIVE,
						false,
						true,
						null,
						LocalDateTime.of(2026, 6, 17, 13, 0)));

		mockMvc.perform(get("/api/admin/accounts/target-admin")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adminId").value("target-admin"))
				.andExpect(jsonPath("$.email").value("op***@pikume.com"))
				.andExpect(jsonPath("$.loginId").value("op***ne"));
	}

	@Test
	@DisplayName("POST /api/admin/accounts는 임시 패스워드를 한 번만 포함한 생성 결과를 반환한다")
	void createReturnsTemporaryPasswordOnce() throws Exception {
		given(createAdminAccountUseCase.create(any(CreateAdminAccountCommand.class)))
				.willReturn(new CreateAdminAccountResult(
						"조회자1",
						AdminRole.VIEWER,
						AdminAccountStatus.ACTIVE,
						"TempPass1!234567",
						LocalDateTime.of(2026, 6, 18, 13, 0),
						true));

		mockMvc.perform(post("/api/admin/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateAdminAccountRequest(
								"viewer@pikume.com",
								"조회자1",
								AdminRole.VIEWER))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.loginId").doesNotExist())
				.andExpect(jsonPath("$.temporaryLoginId").doesNotExist())
				.andExpect(jsonPath("$.temporaryPassword").value("TempPass1!234567"))
				.andExpect(jsonPath("$.guideEmailSent").value(true))
				.andExpect(jsonPath("$.adminId").doesNotExist());

		then(createAdminAccountUseCase).should().create(any(CreateAdminAccountCommand.class));
	}

	@Test
	@DisplayName("POST /api/admin/accounts는 권한 부족 시 Problem Details를 반환한다")
	void createReturnsProblemDetailsWhenForbidden() throws Exception {
		given(createAdminAccountUseCase.create(any(CreateAdminAccountCommand.class)))
				.willThrow(new AdminException(AdminProblem.FORBIDDEN, "SUPER_ADMIN만 관리자 계정을 생성할 수 있습니다."));

		mockMvc.perform(post("/api/admin/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateAdminAccountRequest(
								"viewer@pikume.com",
								"조회자1",
								AdminRole.VIEWER))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/admin/forbidden"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.instance").value("/api/admin/accounts"));
	}

	private record AdminPrincipalResolver(AdminUserDetails adminUserDetails) implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AdminUserDetails.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return adminUserDetails;
		}
	}
}
