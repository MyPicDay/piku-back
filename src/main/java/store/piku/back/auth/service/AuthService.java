package store.piku.back.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.auth.dto.request.LoginRequest;
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
     * 회원가입을 위한 인증 이메일 발송
     */
    @Transactional
    public void sendSignUpVerificationEmail(String email) {
        String code = null;
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            code = emailService.sendVerificationEmail(email);
        } catch (MessagingException e) {
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILURE);
        }
        saveVerificationCode(email, code, VerificationType.SIGN_UP);
    }

    /**
     * 비밀번호 재설정을 위한 인증 이메일 발송
     */
    @Transactional
    public void sendPasswordResetVerificationEmail(String email) {
        String code = null;
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }

        try {
            code = emailService.sendVerificationEmail(email);
        } catch (MessagingException e) {
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILURE);
        }
        saveVerificationCode(email, code, VerificationType.PASSWORD_RESET);
    }

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

    @Transactional
    public boolean verifyCode(String email, String code, VerificationType type) {

        Verification verification = verificationRepository.findByEmailAndType(email, type)
                .orElseThrow(() -> new AuthException(AuthErrorCode.VERIFICATION_NOT_FOUND));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification);
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }

        if (!verification.getCode().equals(code)) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        verificationRepository.delete(verification);

        return true;
    }
}

