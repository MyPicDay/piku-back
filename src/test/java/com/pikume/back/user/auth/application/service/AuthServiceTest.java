package com.pikume.back.user.auth.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.user.application.port.out.CheckUserUniquenessPort;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.auth.application.dto.ResetPasswordCommand;
import com.pikume.back.user.auth.application.dto.SignUpCommand;
import com.pikume.back.user.auth.application.dto.VerifyEmailCommand;
import com.pikume.back.user.auth.application.port.in.QueryAllowedEmailUseCase;
import com.pikume.back.user.auth.application.port.out.*;
import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.service.EmailVerificationPolicy;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import com.pikume.back.user.auth.application.exception.AuthErrorCode;
import com.pikume.back.user.auth.application.exception.AuthException;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
	private LoadUserAccountPort loadUserAccountPort;
	@Mock
	private CheckUserUniquenessPort checkUserUniquenessPort;
	@Mock
	private SaveUserPort saveUserPort;
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
	private PasswordProtectionPort passwordProtectionPort;
	@Mock
	private LoadFixedCharacterForSignUpPort loadFixedCharacterForSignUpPort;
	@Mock
	private QueryAllowedEmailUseCase queryAllowedEmailUseCase;
	@Spy
	private EmailVerificationPolicy emailVerificationPolicy = new EmailVerificationPolicy();

	@Nested
	@DisplayName("signup")
	class Signup {

		@Test
		@DisplayName("유효한 요청으로 회원가입에 성공한다")
		void signupSuccess() throws Exception {
			SignUpCommand dto = new SignUpCommand("test@piku.store", "abc@123", "테스트", 1L);

			given(checkUserUniquenessPort.existsByEmail("test@piku.store")).willReturn(false);

			VerifiedEmail verified = new VerifiedEmail("test@piku.store", VerificationType.SIGN_UP);
			Field idField = VerifiedEmail.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(verified, 1L);

			given(
					loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(verified));
			given(passwordProtectionPort.protect("abc@123")).willReturn("encodedPw");
			given(loadFixedCharacterForSignUpPort.findFixedCharacterObjectKey(1L))
					.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));
			given(saveUserPort.save(any(User.class))).willReturn(null);

			authService.signup(dto);

			then(saveUserPort).should().save(argThat(user ->
					"public/characters/fixed/base_image_1.webp".equals(user.getAvatar())));
			then(saveVerifiedEmailPort).should().save(verified);
		}

		@Test
		@DisplayName("존재하지 않는 고정 캐릭터로 회원가입 시 예외가 발생하고 저장하지 않는다")
		void signupFailFixedCharacterNotFound() throws Exception {
			SignUpCommand dto = new SignUpCommand("test@piku.store", "abc@123", "테스트", 999L);
			given(checkUserUniquenessPort.existsByEmail("test@piku.store")).willReturn(false);

			VerifiedEmail verified = new VerifiedEmail("test@piku.store", VerificationType.SIGN_UP);
			Field idField = VerifiedEmail.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(verified, 1L);

			given(
					loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store", VerificationType.SIGN_UP))
					.willReturn(Optional.of(verified));
			given(loadFixedCharacterForSignUpPort.findFixedCharacterObjectKey(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.signup(dto))
					.isInstanceOfSatisfying(AuthException.class,
							ex -> assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.FIXED_CHARACTER_NOT_FOUND));

			then(saveVerifiedEmailPort).should(never()).save(any());
			then(saveUserPort).should(never()).save(any());
		}

		@Test
		@DisplayName("이미 존재하는 이메일로 회원가입 시 예외가 발생한다")
		void signupFailDuplicateEmail() {
			SignUpCommand dto = new SignUpCommand("dup@piku.store", "abc@123", "테스트", 1L);
			given(checkUserUniquenessPort.existsByEmail("dup@piku.store")).willReturn(true);

			assertThatThrownBy(() -> authService.signup(dto))
					.isInstanceOf(AuthException.class);

			then(saveUserPort).should(never()).save(any());
		}

		@Test
		@DisplayName("회원가입 저장 경쟁의 이메일 충돌을 계정 오류로 변환한다")
		void signupTranslatesEmailConflictFromPersistence() {
			SignUpCommand command = new SignUpCommand("race@piku.store", "abc@123", "테스트", 1L);
			VerifiedEmail verified = new VerifiedEmail("race@piku.store", VerificationType.SIGN_UP);
			given(checkUserUniquenessPort.existsByEmail("race@piku.store")).willReturn(false);
			given(loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc(
					"race@piku.store", VerificationType.SIGN_UP)).willReturn(Optional.of(verified));
			given(loadFixedCharacterForSignUpPort.findFixedCharacterObjectKey(1L))
					.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));
			given(passwordProtectionPort.protect("abc@123")).willReturn("encodedPw");
			given(saveUserPort.save(any(User.class))).willThrow(new EmailAlreadyExistsException());

			assertThatThrownBy(() -> authService.signup(command))
					.isInstanceOfSatisfying(AuthException.class,
							exception -> assertThat(exception.getErrorCode())
									.isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS));
		}

		@Test
		@DisplayName("이메일 인증이 없으면 회원가입 시 예외가 발생한다")
		void signupFailNoVerification() {
			SignUpCommand dto = new SignUpCommand("test@piku.store", "abc@123", "테스트", 1L);
			given(checkUserUniquenessPort.existsByEmail("test@piku.store")).willReturn(false);
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
			given(queryAllowedEmailUseCase.isEmailAllowed("test@piku.store")).willReturn(true);
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
			VerifyEmailCommand dto = new VerifyEmailCommand("test@piku.store", "123456", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP,
					LocalDateTime.now().plusMinutes(5));
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
			VerifyEmailCommand dto = new VerifyEmailCommand("test@piku.store", "123456", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP,
					LocalDateTime.now().plusMinutes(5));

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
			VerifyEmailCommand dto = new VerifyEmailCommand("test@piku.store", "999999", VerificationType.SIGN_UP);
			Verification v = new Verification("test@piku.store", "123456", VerificationType.SIGN_UP,
					LocalDateTime.now().plusMinutes(5));
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
			ResetPasswordCommand dto = new ResetPasswordCommand("test@piku.store", "newPwd@1");
			User user = new User("test@piku.store", "oldPw", "nick");
			given(loadUserAccountPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));

			VerifiedEmail verified = new VerifiedEmail("test@piku.store", VerificationType.PASSWORD_RESET);
			Field idField = VerifiedEmail.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(verified, 1L);
			given(loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc("test@piku.store",
					VerificationType.PASSWORD_RESET))
					.willReturn(Optional.of(verified));
			given(passwordProtectionPort.protect("newPwd@1")).willReturn("encodedNew");

			authService.verifyCodeAndResetPwd(dto);

			then(saveVerifiedEmailPort).should().save(verified);
		}

		@Test
		@DisplayName("존재하지 않는 사용자로 비밀번호 재설정 시 예외가 발생한다")
		void resetFailUserNotFound() {
			ResetPasswordCommand dto = new ResetPasswordCommand("unknown@piku.store", "newPwd@1");
			given(loadUserAccountPort.findByEmail("unknown@piku.store")).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.verifyCodeAndResetPwd(dto))
					.isInstanceOf(AuthException.class);
		}
	}
}
