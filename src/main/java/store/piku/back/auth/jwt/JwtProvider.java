package store.piku.back.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.global.config.CustomUserDetailService;
import store.piku.back.global.config.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;


@Slf4j
@Component
public class JwtProvider {

    private final CustomUserDetailService customUserDetailService;

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtProvider(CustomUserDetailService customUserDetailService) {
        this.customUserDetailService = customUserDetailService;
    }

    /*
    * JWT Access Token 생성
    * */
    public String generateAccessToken(String email) {
        log.info("[JWT Access Token 생성] 이메일 : {}", email);

      Claims claims = Jwts.claims().setSubject(email);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + AuthConstants.ACCESS_TOKEN_EXPIRATION_TIME);
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        claims.put("roles", List.of("ROLE_USER"));

        log.debug("[JWT Access Token 생성] 완료 : 만료시간={}", expiry);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }


    /*
     * JWT Refresh Token 생성
     * */
    public String generateRefreshToken() {
        log.info("[JWT Refresh Token 생성] 생성 시작");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME);
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        log.debug("[JWT Refresh Token 생성] 완료 : 만료시간={}", expiry);

        return Jwts.builder()
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    /*
    * JWT에서 이메일 추출
    * */
    public String getEmailFromToken(String token) {
        token = cleanToken(token);
        log.debug("[JWT 파싱] 이메일 추출 시작");

        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        String email = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        log.debug("[JWT 파싱 완료] 이메일: {}", email);
        return email;
    }

    /*
    * 토큰 유효성 검사
    * */
    public boolean validateToken(String token) {
        try {
            token = cleanToken(token);
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            log.debug("[JWT 유효성 검사] 유효성 검사 통과");
            return true;

        } catch (Exception e) {
            log.warn("[JWT 유효성 검사 실패] 에러: {}", e.getMessage());
            return false;
        }
    }

    public String cleanToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public Authentication getAuthentication(String token) {
        token = cleanToken(token);
        String email = getEmailFromToken(token);

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }


}

