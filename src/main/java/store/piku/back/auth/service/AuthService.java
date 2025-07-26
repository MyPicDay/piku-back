package store.piku.back.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.auth.dto.request.EmailValidRequest;
import store.piku.back.auth.dto.request.LoginRequest;
import store.piku.back.auth.dto.request.PwdResetRequest;
import store.piku.back.auth.dto.request.SignupRequest;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.entity.RefreshToken;
import store.piku.back.auth.entity.Verification;
import store.piku.back.auth.enums.VerificationType;
import store.piku.back.auth.exception.AuthErrorCode;
import store.piku.back.auth.exception.AuthException;
import store.piku.back.auth.repository.VerificationRepository;
import store.piku.back.auth.jwt.JwtProvider;
import store.piku.back.auth.repository.RefreshTokenRepository;
import store.piku.back.character.service.CharacterService;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import store.piku.back.user.service.reader.UserReader;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CharacterService characterService;
    private final EmailService emailService;
    private final UserReader userReader;

    /**
     * 회원가입
     */
    public void signup(SignupRequest dto) {
        log.info("[회원 가입] 서비스 호출 : 이메일={}, 닉네임={}", dto.getEmail(), dto.getNickname());

        // 이메일 존재 여부 확인
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            log.warn("[회원가입] 이미 존재하는 이메일 요청 : 이메일={}", dto.getEmail());
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = new User(
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getNickname()
        );
        String avatarUrl = characterService.getFixedCharacterImageUrl(dto.getFixedCharacterId());
        user.changeAvatar(avatarUrl);
        userRepository.save(user);
        log.info("[회원 가입] 완료 : 이메일={}, 닉네임={}", dto.getEmail(), dto.getNickname());
    }


    public void validateLoginPassword(String requestPassword, String storedPassword, String email) {
        if (!passwordEncoder.matches(requestPassword, storedPassword)) {
            log.warn("[로그인] 실패 - 비밀번호 불일치 : 이메일={}", email);
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        log.info("[비밀번호 검증 성공] 비밀번호가 일치합니다.");
    }
    /**
     * 로그인
     */
    public TokenDto login(LoginRequest dto, String deviceId) {
        log.info("[로그인] 서비스 호출 : 이메일={}", dto.getEmail());
        String keyId = dto.getEmail() + "-" + deviceId;

        User user = userReader.getUserByEmail(dto.getEmail());

        validateLoginPassword(dto.getPassword(), user.getPassword(), dto.getEmail());

        log.info("[로그인] 완료 : 이메일={}", dto.getEmail());
        String accessToken = getNewAccessToken(dto.getEmail());
        String refreshToken = getNewRefreshToken(dto.getEmail(), deviceId, user.getId());

        log.info("[JWT Refresh Token 저장 완료] key={}, refreshToken={}", keyId, refreshToken);

        return new TokenDto(accessToken, refreshToken);
    }

    public String getNewAccessToken(String email) {
        String newAccessToken = jwtProvider.generateAccessToken(email);
        return newAccessToken;
    }

    private String getNewRefreshToken(String email, String deviceId, String userId) {
        String keyId = email + "-" + deviceId;
        String newRefreshToken = jwtProvider.generateRefreshToken();
        RefreshToken refreshTokenEntity = new RefreshToken(keyId, newRefreshToken, userId);
        refreshTokenRepository.save(refreshTokenEntity);
        log.info("[JWT Refresh Token 저장 완료] key={}, refresh Token={}", keyId, newRefreshToken);
        return newRefreshToken;
    }

    @Transactional
    public String reissueAccessToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            refreshTokenRepository.deleteByRefreshToken(refreshToken);
            return null;
        }

        // 저장된 refreshToken에서 email 추출
        RefreshToken tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("저장된 리프레시 토큰 없음"));

        String email = tokenEntity.getKey().split("-")[0];

        // 새 Access Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(email);
        return newAccessToken;
    }

    public ResponseCookie removeCookieRefreshToken() {
        return ResponseCookie.from(AuthConstants.REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie newCookieRefreshToken(String refreshToken) {
        return ResponseCookie.from(AuthConstants.REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME)
                .sameSite("Lax")
                .build();
    }



    public UserInfo getUserInfoByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return new UserInfo(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getNickname(),
                user.getAvatar()
        );
    }



    /**
     * 회원가입을 위한 인증 이메일을 발송합니다.
     * 해당 이메일이 이미 사용 중인지 확인하고, 사용 가능할 경우 인증 코드를 생성하여 발송합니다.
     *
     * @param email 검증 및 인증 코드 발송 대상 이메일
     * @throws AuthException 이미 가입된 이메일이거나 메일 서버 문제로 발송에 실패할 경우
     */
    @Transactional
    public void sendSignUpVerificationEmail(String email) {
        // 이메일 검증
        if (!emailService.isEmailAllowed(email)) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL);
        }

        String code;
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            code = emailService.sendVerificationEmail(email);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILURE);
        }
        saveVerificationCode(email, code, VerificationType.SIGN_UP);
    }

    /**
     * 비밀번호 재설정을 위한 인증 이메일을 발송합니다.
     * 가입된 사용자인지 확인 후, 인증 코드를 생성하여 이메일로 발송합니다.
     *
     * @param email 인증 코드를 발송할 가입된 사용자의 이메일
     * @throws AuthException 해당 이메일로 가입된 사용자가 없거나, 메일 서버 문제로 발송에 실패할 경우
     */
    @Transactional
    public void sendPasswordResetVerificationEmail(String email) {
        String code;
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }

        try {
            code = emailService.sendVerificationEmail(email);
        }  catch (MessagingException | UnsupportedEncodingException e) {
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILURE);
        }
        saveVerificationCode(email, code, VerificationType.PASSWORD_RESET);
    }

    /**
     * 인증 코드를 데이터베이스에 저장합니다.
     * 동일한 이메일과 인증 목적(type)을 가진 기존 인증 정보가 있으면 코드를 새로 발급하고 만료 시간을 갱신합니다.
     *
     * @param email 인증을 진행할 사용자의 이메일
     * @param code  발송된 인증 코드
     * @param type  인증 목적 (회원가입, 비밀번호 재설정 등)
     */
    @Transactional
    public void saveVerificationCode(String email, String code, VerificationType type) {
        Optional<Verification> verificationOpt = verificationRepository.findByEmailAndType(email, type);

        if (verificationOpt.isPresent()) {
            // 기존에 코드가 있으면, 새 코드로 업데이트
            Verification verification = verificationOpt.get();
            verification.updateCode(code);
        } else {
            Verification verification = new Verification(email, code, type);
            verificationRepository.save(verification);
        }
    }

    /**
     * 이메일 인증 코드를 검증합니다.
     * 요청된 이메일, 코드, 목적(type)에 해당하는 인증 정보가 유효한지 확인합니다.
     * 성공 시 해당 인증 정보는 DB에서 삭제되며, 실패 시 각 상황에 맞는 예외를 발생시킵니다.
     *
     * @param dto  사용자가 입력한 이메일과 인증 코드를 담은 DTO
     * @param type 인증 목적 (회원가입, 비밀번호 재설정 등)
     * @throws AuthException 인증 요청이 존재하지 않거나, 코드가 만료되거나, 코드가 일치하지 않을 경우 발생
     */
    @Transactional
    public void verifyCode(EmailValidRequest dto, VerificationType type) {

        Verification verification = verificationRepository.findByEmailAndType(dto.getEmail(), type)
                .orElseThrow(() -> new AuthException(AuthErrorCode.VERIFICATION_NOT_FOUND));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification);
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }

        if (!verification.getCode().equals(dto.getCode())) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        verificationRepository.delete(verification);
    }

    /**
     * 인증 코드를 검증하고 사용자의 비밀번호를 재설정합니다.
     * 비밀번호 재설정용 인증 코드가 유효한지 확인한 후, 성공 시 사용자의 비밀번호를 변경합니다.
     *
     * @param dto 사용자가 입력한 이메일, 인증 코드, 새로운 비밀번호를 담은 DTO
     * @throws AuthException 인증 코드가 유효하지 않을 경우
     */
    @Transactional
    public void verifyCodeAndResetPwd(PwdResetRequest dto) {

        EmailValidRequest emailValidRequest = new EmailValidRequest(dto.getEmail(), dto.getCode());

        verifyCode(emailValidRequest, VerificationType.PASSWORD_RESET);
        User user = userReader.getUserByEmail(dto.getEmail());
        user.updatePassword(passwordEncoder.encode(dto.getPassword()));

    }
}
