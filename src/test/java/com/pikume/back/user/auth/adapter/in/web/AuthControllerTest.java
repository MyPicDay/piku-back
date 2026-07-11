package com.pikume.back.user.auth.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.user.auth.application.port.in.ResetPasswordUseCase;
import com.pikume.back.user.auth.application.port.in.SignUpUseCase;
import com.pikume.back.user.auth.application.port.in.VerifyEmailUseCase;
import com.pikume.back.user.auth.application.port.out.SendVerificationEmailPort;
import com.pikume.back.user.auth.dto.request.SignupRequest;
import com.pikume.back.user.auth.exception.AuthErrorCode;
import com.pikume.back.user.auth.exception.AuthException;
import com.pikume.back.user.auth.exception.AuthExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

	@InjectMocks
	private AuthController authController;

	@Mock
	private SignUpUseCase signUpUseCase;

	@Mock
	private VerifyEmailUseCase verifyEmailUseCase;

	@Mock
	private ResetPasswordUseCase resetPasswordUseCase;

	@Mock
	private SendVerificationEmailPort sendVerificationEmailPort;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		authController = new AuthController(
				signUpUseCase,
				verifyEmailUseCase,
				resetPasswordUseCase,
				sendVerificationEmailPort);
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(
						new GlobalExceptionHandler(Optional.empty(), problemDetailFactory),
						new AuthExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("POST /api/auth/send-verification/sign-up은 기존 성공 메시지를 반환한다")
	void sendSignUpVerificationEmailReturnsMessageResponse() throws Exception {
		mockMvc.perform(post("/api/auth/send-verification/sign-up")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("회원가입 인증 이메일이 발송되었습니다."));

		then(verifyEmailUseCase).should().sendSignUpVerificationEmail("user@example.com");
	}

	@Test
	@DisplayName("POST /api/auth/send-verification/password-reset은 기존 성공 메시지를 반환한다")
	void sendPasswordResetVerificationEmailReturnsMessageResponse() throws Exception {
		mockMvc.perform(post("/api/auth/send-verification/password-reset")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("비밀번호 재설정 인증 이메일이 발송되었습니다."));

		then(verifyEmailUseCase).should().sendPasswordResetVerificationEmail("user@example.com");
	}

	@Test
	@DisplayName("POST /api/auth/verify-code는 기존 성공 메시지를 반환한다")
	void verifyCodeReturnsMessageResponse() throws Exception {
		mockMvc.perform(post("/api/auth/verify-code")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\",\"code\":\"123456\",\"type\":\"SIGN_UP\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

		then(verifyEmailUseCase).should().verifyCode(any());
	}

	@Test
	@DisplayName("POST /api/auth/password-reset은 기존 성공 메시지를 반환한다")
	void resetPasswordReturnsMessageResponse() throws Exception {
		mockMvc.perform(post("/api/auth/password-reset")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\",\"password\":\"newPassword1!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("비밀번호가 재설정되었습니다."));

		then(resetPasswordUseCase).should().verifyCodeAndResetPwd(any());
	}

	@Test
	@DisplayName("GET /api/auth/email은 이메일 허용 여부 계약을 유지한다")
	void isEmailAllowedReturnsAllowedFlag() throws Exception {
		given(sendVerificationEmailPort.isEmailAllowed("user@example.com")).willReturn(true);

		mockMvc.perform(get("/api/auth/email").param("email", "user@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allowed").value(true));
	}

	@Test
	@DisplayName("GET /api/auth/email-domains는 허용 도메인 목록 계약을 유지한다")
	void getAllowedEmailDomainsReturnsDomainList() throws Exception {
		given(sendVerificationEmailPort.getAllowedEmailDomains()).willReturn(List.of("example.com", "pikume.com"));

		mockMvc.perform(get("/api/auth/email-domains"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value("example.com"))
				.andExpect(jsonPath("$[1]").value("pikume.com"));
	}

	@Test
	@DisplayName("POST /api/auth/signup은 성공 시 MessageResponse를 반환한다")
	void signupReturnsMessageResponseWhenSuccessful() throws Exception {
		SignupRequest request = new SignupRequest("user@example.com", "abc@123", "pikume", 1L);
		doNothing().when(signUpUseCase).signup(any(SignupRequest.class));

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value(new MessageResponse("회원가입 성공").message()));
	}

	@Test
	@DisplayName("POST /api/auth/signup은 AuthException 발생 시 Problem Details를 반환한다")
	void signupReturnsProblemDetailWhenAuthExceptionOccurs() throws Exception {
		SignupRequest request = new SignupRequest("user@example.com", "abc@123", "pikume", 1L);
		willThrow(new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS))
				.given(signUpUseCase)
				.signup(any(SignupRequest.class));

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/auth/email-already-exists"))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("이미 가입된 이메일입니다."))
				.andExpect(jsonPath("$.instance").value("/api/auth/signup"));
	}

	@Test
	@DisplayName("POST /api/auth/signup은 존재하지 않는 고정 캐릭터면 Problem Details를 반환한다")
	void signupReturnsProblemDetailWhenFixedCharacterNotFound() throws Exception {
		SignupRequest request = new SignupRequest("user@example.com", "abc@123", "pikume", 999L);
		willThrow(new AuthException(AuthErrorCode.FIXED_CHARACTER_NOT_FOUND))
				.given(signUpUseCase)
				.signup(any(SignupRequest.class));

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/auth/fixed-character-not-found"))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("존재하지 않는 캐릭터입니다."))
				.andExpect(jsonPath("$.instance").value("/api/auth/signup"));
	}
}
