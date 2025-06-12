package store.piku.back.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.auth.dto.LoginRequest;
import store.piku.back.auth.dto.SignupRequest;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.entity.RefreshToken;
import store.piku.back.auth.jwt.JwtProvider;
import store.piku.back.auth.repository.RefreshTokenRepository;
import store.piku.back.character.service.CharacterService;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CharacterService characterService;

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

    /**
     * 로그인
     */
    public TokenDto login(LoginRequest dto, String deviceId) {
        log.info("[로그인] 서비스 호출 : 이메일={}", dto.getEmail());
        String keyId = dto.getEmail() + "-" + deviceId;

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.warn("[로그인] 실패 - 존재하지 않는 사용자 : 이메일={}", dto.getEmail());
                    return new RuntimeException("존재하지 않는 사용자입니다.");
                });

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("[로그인] 실패 - 비밀번호 불일치 : 이메일={}", dto.getEmail());
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        log.info("[로그인] 완료 : 이메일={}", dto.getEmail());
        String accessToken = jwtProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken();

        RefreshToken refreshTokenEntity = new RefreshToken(keyId, refreshToken);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("[JWT Refresh Token 저장 완료] key={}, refreshToken={}", keyId, refreshToken);

        return new TokenDto(accessToken, refreshToken);
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
}

