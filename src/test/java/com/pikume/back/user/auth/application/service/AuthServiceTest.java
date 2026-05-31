package com.pikume.back.user.auth.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.auth.application.port.out.*;
import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import com.pikume.back.user.auth.dto.request.EmailValidRequest;
import com.pikume.back.user.auth.dto.request.PwdResetRequest;
import com.pikume.back.user.auth.dto.request.SignupRequest;
import com.pikume.back.user.auth.exception.AuthException;
import com.pikume.back.user.domain.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private LoadUserForSignUpPort loadUserForSignUpPort;
	@Mock
	private LoadVerificationPort loadVerificationPort;
	@Mock
	private SaveVerificationPort saveVerificationPort;
	@Mock
	private LoadVerifiedEmailPort loadVerifiedEmailPort;
	@Mock
	private SaveVerifiedEmailPort saveVerifiedEmailPort;
	@Mock
	private SendVerificationEmailPort sendVerificationEmailPort;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private GetCharacterUseCase getCharacterUseCase;

	@Nested
	@DisplayName("signup")
	class Signup {

		@Test
		@DisplayName("유효한 요청으로 회원가입에 성공한다")
		void signupSuccess() throws Exception {
			SignupRequest dto = new SignupRequest("test@piku.store", "abc@123", "테스트", 1L);

			given(loadUserForSignUpPort.findByEmail("test@piku.store")).willReturn(Optional.empty());

			VerifiedEmail verified = new VerifiedEmail("test@piku.store", VerificationType.SIGN_UP);
			Field idField = VerifiedEmail.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(verified, 1L);

			given(
					loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(verified));
			given(passwordEncoder.encode("abc@123")).willReturn("encodedPw");
			given(getCharacterUseCase.getFixedCharacterObjectKey(1L))
					.willReturn("public/characters/fixed/base_image_1.png");
			given(loadUserForSignUpPort.save(any(User.class))).willReturn(null);

			authService.signup(dto);

			then(loadUserForSignUpPort).should().save(argThat(user ->
					"public/characters/fixed/base_image_1.png".equals(user.getAvatar())));
			then(saveVerifiedEmailPort).should().save(verified);
		}

		@Test
		@DisplayName("이미 존재하는 이메일로 회원가입 시 예외가 발생한다")
		void signupFailDuplicateEmail() {
			SignupRequest dto = new SignupRequest("dup@piku.store", "abc@123", "테스트", 1L);
			given(loadUserForSignUpPort.findByEmail("dup@piku.store"))
					.willReturn(Optional.of(new User("dup@piku.store", "pw", "nick")));

			assertThatThrownBy(() -> authService.signup(dto))
					.isInstanceOf(AuthException.class);

			then(loadUserForSignUpPort).should(never()).save(any());
		}

		@Test
		@DisplayName("이메일 인증이 없으면 회원가입 시 예외가 발생한다")
		void signupFailNoVerification() {
			SignupRequest dto = new SignupRequest("test@piku.store", "abc@123", "테스트", 1L);
			given(loadUserForSignUpPort.findByEmail("test@piku.store")).willReturn(Optional.empty());
			given(
					loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.signup(dto))
					.isInstanceOf(AuthException.class);
		}
	}

	@Nested
	@DisplayName("sendSignUpVerificationEmail")
	class SendSignUpVerification {

		@Test
		@DisplayName("인증 이메일 발송에 성공한다")
		void sendSuccess() {
			given(sendVerificationEmailPort.isEmailAllowed("test@piku.store")).willReturn(true);
			given(sendVerificationEmailPort.sendVerificationEmail("test@piku.store")).willReturn("123456");

			authService.sendSignUpVerificationEmail("test@piku.store");

			then(saveVerificationPort).should().save(any(Verification.class));
		}
	}

	@Nested
	@DisplayName("verifyCode")
	class VerifyCode {

		@Test
		@DisplayName("유효한 인증 코드 검증에 성공한다")
		void verifySuccess() throws Exception {
			EmailValidRequest dto = new EmailValidRequest("test@piku.store", "123456", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP);
			Field idField = Verification.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(v, 1L);

			given(loadVerificationPort.findByEmailAndType("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(v));

			authService.verifyCode(dto);

			then(saveVerificationPort).should().delete(v);
			then(saveVerifiedEmailPort).should().save(any(VerifiedEmail.class));
		}

		@Test
		@DisplayName("만료된 인증 코드로 검증 시 예외가 발생한다")
		void verifyFailExpired() throws Exception {
			EmailValidRequest dto = new EmailValidRequest("test@piku.store", "123456", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP);

			Field expiresField = Verification.class.getDeclaredField("expiresAt");
			expiresField.setAccessible(true);
			expiresField.set(v, LocalDateTime.now().minusMinutes(10));

			Field idField = Verification.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(v, 1L);

			given(loadVerificationPort.findByEmailAndType("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(v));

			assertThatThrownBy(() -> authService.verifyCode(dto))
					.isInstanceOf(AuthException.class);
		}

		@Test
		@DisplayName("불일치 인증 코드로 검증 시 예외가 발생한다")
		void verifyFailMismatch() throws Exception {
			EmailValidRequest dto = new EmailValidRequest("test@piku.store", "999999", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP);
			Field idField = Verification.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(v, 1L);

			given(loadVerificationPort.findByEmailAndType("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(v));

			assertThatThrownBy(() -> authService.verifyCode(dto))
					.isInstanceOf(AuthException.class);
		}
	}

	@Nested
	@DisplayName("verifyCodeAndResetPwd")
	class ResetPassword {

		@Test
		@DisplayName("비밀번호 재설정에 성공한다")
		void resetSuccess() throws Exception {
			PwdResetRequest dto = new PwdResetRequest("test@piku.store", "newPwd@1");
			User user = new User("test@piku.store", "oldPw", "nick");
			given(loadUserForSignUpPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));

			VerifiedEmail verified = new VerifiedEmail("test@piku.store", VerificationType.PASSWORD_RESET);
			Field idField = VerifiedEmail.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(verified, 1L);
			given(loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store",
					VerificationType.PASSWORD_RESET))
					.willReturn(Optional.of(verified));
			given(passwordEncoder.encode("newPwd@1")).willReturn("encodedNew");

			authService.verifyCodeAndResetPwd(dto);

			then(saveVerifiedEmailPort).should().save(verified);
		}

		@Test
		@DisplayName("존재하지 않는 사용자로 비밀번호 재설정 시 예외가 발생한다")
		void resetFailUserNotFound() {
			PwdResetRequest dto = new PwdResetRequest("unknown@piku.store", "newPwd@1");
			given(loadUserForSignUpPort.findByEmail("unknown@piku.store")).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.verifyCodeAndResetPwd(dto))
					.isInstanceOf(AuthException.class);
		}
	}
}
