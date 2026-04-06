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
import com.pikume.back.user.auth.application.port.in.ResetPasswordUseCase;
import com.pikume.back.user.auth.application.port.in.SignUpUseCase;
import com.pikume.back.user.auth.application.port.in.VerifyEmailUseCase;
import com.pikume.back.user.auth.application.port.out.SendVerificationEmailPort;
import com.pikume.back.user.auth.dto.request.SignupRequest;
import com.pikume.back.user.auth.exception.AuthErrorCode;
import com.pikume.back.user.auth.exception.AuthException;
import com.pikume.back.user.auth.exception.AuthExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.willThrow;
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
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(new AuthExceptionHandler(new ProblemDetailFactory()))
				.build();
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
}
