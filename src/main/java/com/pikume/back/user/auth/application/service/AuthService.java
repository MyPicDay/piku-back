package com.pikume.back.user.auth.application.service;

import com.pikume.back.user.application.port.out.CheckUserUniquenessPort;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.auth.application.dto.ResetPasswordCommand;
import com.pikume.back.user.auth.application.dto.SignUpCommand;
import com.pikume.back.user.auth.application.dto.VerifyEmailCommand;
import com.pikume.back.user.auth.application.port.in.QueryAllowedEmailUseCase;
import com.pikume.back.user.auth.application.port.in.ResetPasswordUseCase;
import com.pikume.back.user.auth.application.port.in.SignUpUseCase;
import com.pikume.back.user.auth.application.port.in.VerifyEmailUseCase;
import com.pikume.back.user.auth.application.port.out.LoadFixedCharacterForSignUpPort;
import com.pikume.back.user.auth.application.port.out.LoadVerificationPort;
import com.pikume.back.user.auth.application.port.out.LoadVerifiedEmailPort;
import com.pikume.back.user.auth.application.port.out.PasswordProtectionPort;
import com.pikume.back.user.auth.application.port.out.SaveVerificationPort;
import com.pikume.back.user.auth.application.port.out.SaveVerifiedEmailPort;
import com.pikume.back.user.auth.application.port.out.SendVerificationEmailPort;
import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.service.EmailVerificationPolicy;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import com.pikume.back.user.auth.application.exception.AuthErrorCode;
import com.pikume.back.user.auth.application.exception.AuthException;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements SignUpUseCase, VerifyEmailUseCase, ResetPasswordUseCase {

	private final LoadUserAccountPort loadUserAccountPort;
	private final CheckUserUniquenessPort checkUserUniquenessPort;
	private final SaveUserPort saveUserPort;
	private final LoadVerificationPort loadVerificationPort;
	private final SaveVerificationPort saveVerificationPort;
	private final LoadVerifiedEmailPort loadVerifiedEmailPort;
	private final SaveVerifiedEmailPort saveVerifiedEmailPort;
	private final SendVerificationEmailPort sendVerificationEmailPort;
	private final PasswordProtectionPort passwordProtectionPort;
	private final LoadFixedCharacterForSignUpPort loadFixedCharacterForSignUpPort;
	private final QueryAllowedEmailUseCase queryAllowedEmailUseCase;
	private final EmailVerificationPolicy emailVerificationPolicy;

	@Override
	@Transactional
	public void signup(SignUpCommand command) {
		if (checkUserUniquenessPort.existsByEmail(command.email())) {
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		VerifiedEmail verified = getValidVerifiedEmail(command.email(), VerificationType.SIGN_UP);
		String avatarObjectKey = requireFixedCharacterObjectKey(command.fixedCharacterId());
		User user = new User(
				command.email(),
				passwordProtectionPort.protect(command.password()),
				command.nickname());
		user.changeAvatar(avatarObjectKey);

		verified.markUsed();
		saveVerifiedEmailPort.save(verified);
		try {
			saveUserPort.save(user);
		} catch (EmailAlreadyExistsException exception) {
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		} catch (NicknameAlreadyExistsException exception) {
			throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
		}
		log.info("event=user_signup outcome=success userId={}", user.getId());
	}

	@Override
	@Transactional
	public void sendSignUpVerificationEmail(String email) {
		if (!queryAllowedEmailUseCase.isEmailAllowed(email)) {
			throw new AuthException(AuthErrorCode.INVALID_EMAIL);
		}
		if (checkUserUniquenessPort.existsByEmail(email)) {
			log.info("event=verification_request outcome=accepted reason=email_already_registered");
		}
		saveVerificationCode(email, sendVerificationEmailPort.sendVerificationEmail(email), VerificationType.SIGN_UP);
	}

	@Override
	@Transactional
	public void sendPasswordResetVerificationEmail(String email) {
		if (!checkUserUniquenessPort.existsByEmail(email)) {
			log.info("event=password_reset_verification outcome=accepted reason=email_not_registered");
		}
		saveVerificationCode(
				email,
				sendVerificationEmailPort.sendVerificationEmail(email),
				VerificationType.PASSWORD_RESET);
	}

	@Override
	@Transactional
	public void verifyCode(VerifyEmailCommand command) {
		Verification verification = loadVerificationPort.findByEmailAndType(command.email(), command.type())
				.orElseThrow(() -> new AuthException(AuthErrorCode.VERIFICATION_NOT_FOUND));
		LocalDateTime now = LocalDateTime.now();
		if (emailVerificationPolicy.isCodeExpired(verification.getExpiresAt(), now)) {
			saveVerificationPort.delete(verification);
			throw new AuthException(AuthErrorCode.CODE_EXPIRED);
		}
		if (!verification.matches(command.code(), command.type())) {
			throw new AuthException(AuthErrorCode.CODE_MISMATCH);
		}

		saveVerificationPort.delete(verification);
		saveVerifiedEmailPort.save(new VerifiedEmail(command.email(), command.type()));
	}

	@Override
	@Transactional
	public void verifyCodeAndResetPwd(ResetPasswordCommand command) {
		User user = loadUserAccountPort.findByEmail(command.email())
				.orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
		VerifiedEmail verified = getValidVerifiedEmail(command.email(), VerificationType.PASSWORD_RESET);

		verified.markUsed();
		saveVerifiedEmailPort.save(verified);
		user.updatePassword(passwordProtectionPort.protect(command.newPassword()));
		saveUserPort.save(user);
		log.info("event=password_reset outcome=success userId={}", user.getId());
	}

	private void saveVerificationCode(String email, String code, VerificationType type) {
		LocalDateTime expiresAt = emailVerificationPolicy.codeExpiresAt(LocalDateTime.now());
		Verification verification = loadVerificationPort.findByEmailAndType(email, type)
				.orElseGet(() -> new Verification(email, code, type, expiresAt));
		if (verification.getId() != null) {
			verification.updateCode(code, expiresAt);
		}
		saveVerificationPort.save(verification);
	}

	private VerifiedEmail getValidVerifiedEmail(String email, VerificationType type) {
		VerifiedEmail latest = loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc(email, type)
				.orElseThrow(() -> new AuthException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND));
		if (!latest.isFor(email, type)) {
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
		}
		if (emailVerificationPolicy.isCompletedVerificationExpired(latest.getVerifiedAt(), LocalDateTime.now())) {
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
		}
		if (latest.isUsed()) {
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_ALREADY_USED);
		}
		return latest;
	}

	private String requireFixedCharacterObjectKey(Long fixedCharacterId) {
		if (fixedCharacterId == null || fixedCharacterId <= 0) {
			throw new AuthException(AuthErrorCode.FIXED_CHARACTER_NOT_FOUND);
		}
		return loadFixedCharacterForSignUpPort.findFixedCharacterObjectKey(fixedCharacterId)
				.orElseThrow(() -> new AuthException(AuthErrorCode.FIXED_CHARACTER_NOT_FOUND));
	}
}
