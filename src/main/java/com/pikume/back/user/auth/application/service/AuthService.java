package com.pikume.back.user.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.auth.application.port.in.ResetPasswordUseCase;
import com.pikume.back.user.auth.application.port.in.SignUpUseCase;
import com.pikume.back.user.auth.application.port.in.VerifyEmailUseCase;
import com.pikume.back.user.auth.application.port.out.*;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.user.auth.domain.Verification;
import com.pikume.back.user.auth.domain.VerifiedEmail;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import com.pikume.back.user.auth.dto.request.EmailValidRequest;
import com.pikume.back.user.auth.dto.request.PwdResetRequest;
import com.pikume.back.user.auth.dto.request.SignupRequest;
import com.pikume.back.user.auth.exception.AuthErrorCode;
import com.pikume.back.user.auth.exception.AuthException;
import com.pikume.back.user.domain.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements SignUpUseCase, VerifyEmailUseCase, ResetPasswordUseCase {

	private final LoadUserForSignUpPort loadUserForSignUpPort;
	private final LoadVerificationPort loadVerificationPort;
	private final SaveVerificationPort saveVerificationPort;
	private final LoadVerifiedEmailPort loadVerifiedEmailPort;
	private final SaveVerifiedEmailPort saveVerifiedEmailPort;
	private final SendVerificationEmailPort sendVerificationEmailPort;
	private final PasswordEncoder passwordEncoder;
	private final GetCharacterUseCase getCharacterUseCase;

	@Override
	@Transactional
	public void signup(SignupRequest dto) {
		log.info("[회원 가입] 서비스 호출 : 이메일={}, 닉네임={}", dto.getEmail(), dto.getNickname());

		if (loadUserForSignUpPort.findByEmail(dto.getEmail()).isPresent()) {
			log.warn("[회원가입] 이미 존재하는 이메일 요청 : 이메일={}", dto.getEmail());
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		VerifiedEmail verified = getValidVerifiedEmail(dto.getEmail(), VerificationType.SIGN_UP);
		log.info("[회원가입] 이메일 인증 정보 확인 완료. verifiedId={}", verified.getId());

		verified.markUsed();
		saveVerifiedEmailPort.save(verified);
		log.info("[회원가입] 이메일 인증 정보 사용 처리 완료.");

		User user = new User(
				dto.getEmail(),
				passwordEncoder.encode(dto.getPassword()),
				dto.getNickname());
		String avatarUrl = getCharacterUseCase.getFixedCharacterImageUrl(dto.getFixedCharacterId());
		user.changeAvatar(avatarUrl);
		loadUserForSignUpPort.save(user);
		log.info("[회원 가입] 완료 : 이메일={}, 닉네임={}", dto.getEmail(), dto.getNickname());
	}

	@Override
	@Transactional
	public void sendSignUpVerificationEmail(String email) {
		if (!sendVerificationEmailPort.isEmailAllowed(email)) {
			throw new AuthException(AuthErrorCode.INVALID_EMAIL);
		}

		if (loadUserForSignUpPort.existsByEmail(email)) {
			log.info("이미 가입된 이메일로 인증 요청 감지. email= {}", email);
		}

		String code = sendVerificationEmail(email);
		log.info("회원가입을 위한 인증 이메일을 발송합니다. email= {}", email);
		saveVerificationCode(email, code, VerificationType.SIGN_UP);
	}

	@Override
	@Transactional
	public void sendPasswordResetVerificationEmail(String email) {
		if (!loadUserForSignUpPort.existsByEmail(email)) {
			log.info("가입되지 않은 이메일로 비밀번호 재설정을 요청 감지. email= {}", email);
		}

		String code = sendVerificationEmail(email);
		log.info("비밀번호 재설정을 위한 인증 이메일을 발송합니다. email= {}", email);
		saveVerificationCode(email, code, VerificationType.PASSWORD_RESET);
	}

	@Override
	@Transactional
	public void verifyCode(EmailValidRequest dto) {
		Verification verification = loadVerificationPort.findByEmailAndType(dto.getEmail(), dto.getType())
				.orElseThrow(() -> {
					log.warn("[코드 검증] 해당 이메일의 인증 요청 정보를 찾을 수 없음. email={}", dto.getEmail());
					return new AuthException(AuthErrorCode.VERIFICATION_NOT_FOUND);
				});
		log.info("[코드 검증] DB에서 인증 요청 정보를 찾았습니다. verificationId={}", verification.getId());

		if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
			log.warn("[코드 검증] 인증 코드가 만료되었습니다. verificationId={}", verification.getId());
			saveVerificationPort.delete(verification);
			throw new AuthException(AuthErrorCode.CODE_EXPIRED);
		}

		if (!verification.getCode().equals(dto.getCode())) {
			log.warn("[코드 검증] 인증 코드가 일치하지 않습니다. verificationId={}", verification.getId());
			throw new AuthException(AuthErrorCode.CODE_MISMATCH);
		}
		log.info("[코드 검증] 코드 일치 확인 완료. verificationId={}", verification.getId());

		saveVerificationPort.delete(verification);
		saveVerifiedEmailPort.save(new VerifiedEmail(dto.getEmail(), dto.getType()));
	}

	@Override
	@Transactional
	public void verifyCodeAndResetPwd(PwdResetRequest dto) {
		User user = loadUserForSignUpPort.findByEmail(dto.getEmail())
				.orElseThrow(() -> {
					log.warn("[비밀번호 재설정] 존재하지 않는 이메일로 비밀번호 재설정 요청 : 이메일={}", dto.getEmail());
					return new AuthException(AuthErrorCode.USER_NOT_FOUND);
				});

		VerifiedEmail verified = getValidVerifiedEmail(dto.getEmail(), VerificationType.PASSWORD_RESET);
		log.info("[비밀번호 재설정] 이메일 인증 정보 확인 완료. verifiedId={}", verified.getId());

		verified.markUsed();
		saveVerifiedEmailPort.save(verified);

		user.updatePassword(passwordEncoder.encode(dto.getPassword()));
		log.info("[비밀번호 재설정] 완료. userId={}", user.getId());
	}

	private void saveVerificationCode(String email, String code, VerificationType type) {
		log.info("[인증 코드 저장] 서비스 호출. email={}, type={}", email, type);

		Optional<Verification> verificationOpt = loadVerificationPort.findByEmailAndType(email, type);

		if (verificationOpt.isPresent()) {
			Verification verification = verificationOpt.get();
			log.info("[인증 코드 저장] 기존 인증 정보를 새 코드로 갱신합니다. verificationId={}", verification.getId());
			verification.updateCode(code);
		} else {
			Verification verification = new Verification(email, code, type);
			saveVerificationPort.save(verification);
			log.info("[인증 코드 저장] 신규 인증 정보 저장 완료. verificationId={}", verification.getId());
		}
	}

	private String sendVerificationEmail(String email) {
		try {
			return sendVerificationEmailPort.sendVerificationEmail(email);
		} catch (AuthException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILURE);
		}
	}

	public VerifiedEmail getValidVerifiedEmail(String email, VerificationType type) {
		VerifiedEmail latest = loadVerifiedEmailPort.findTopByEmailAndTypeOrderByVerifiedAtDesc(email, type)
				.orElseThrow(() -> {
					log.warn("[이메일 인증 실패] 인증 기록 없음 : 이메일={}, type={}", email, type);
					return new AuthException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
				});

		LocalDateTime cutoff = LocalDateTime.now().minusMinutes(AuthConstants.VERIFICATION_EXPIRATION_MINUTES);
		if (latest.getVerifiedAt().isBefore(cutoff)) {
			log.warn("[이메일 인증 실패] 인증 만료됨 : 이메일={}, type={}", email, type);
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
		}

		if (latest.getUsed()) {
			log.warn("[이메일 인증 실패] 인증 이미 사용됨 : 이메일={}, type={}", email, type);
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_ALREADY_USED);
		}

		return latest;
	}
}
