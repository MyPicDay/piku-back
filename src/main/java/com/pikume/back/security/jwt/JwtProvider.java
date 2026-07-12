package com.pikume.back.security.jwt;

import com.pikume.back.security.adapter.in.web.AuthWebConstants;
import com.pikume.back.security.config.UserTokenSettings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	/*
	 * JWT Access Token 생성
	 */
	public String generateAccessToken(String userId) {
		log.debug("event=access_token_generation_requested userId={}", userId);

		Claims claims = Jwts.claims().setSubject(userId);
		Date now = new Date();
		Date expiry = new Date(now.getTime() + UserTokenSettings.ACCESS_TOKEN_EXPIRATION_MILLIS);

		claims.put("roles", List.of("ROLE_USER"));

		log.debug("event=access_token_generated userId={} expiresAt={}", userId, expiry);

		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(signingKey())
				.compact();
	}


	/*
	 * JWT Refresh Token 생성
	 */
	public String generateRefreshToken() {
		log.debug("event=refresh_token_generation_requested");

		Date now = new Date();
		Date expiry = new Date(now.getTime() + UserTokenSettings.REFRESH_TOKEN_EXPIRATION_MILLIS);

		log.debug("event=refresh_token_generated expiresAt={}", expiry);

		return Jwts.builder()
				.setExpiration(expiry)
				.signWith(signingKey())
				.compact();
	}

	/*
	 * JWT에서 사용자 ID 추출
	 */
	public String getUserIdFromToken(String token) {
		token = cleanToken(token);
		log.debug("event=jwt_subject_parse_requested");

		String userId = parseClaims(token).getSubject();
		if (userId == null || userId.isBlank()) {
			throw new BadCredentialsException("사용자 ID가 없는 토큰입니다.");
		}

		log.debug("event=jwt_subject_parsed userId={}", userId);
		return userId;
	}


	/*
	 * 토큰 유효성 검사
	 */
	public boolean validateToken(String token) {
		try {
			token = cleanToken(token);
			parseClaims(token);
			log.debug("event=jwt_validation_succeeded");
			return true;

		} catch (Exception e) {
			log.debug("event=jwt_validation_failed reason={}", e.getClass().getSimpleName());
			return false;
		}
	}

	public String cleanToken(String token) {
		if (token != null && token.startsWith(AuthWebConstants.BEARER_PREFIX)) {
			return token.substring(AuthWebConstants.BEARER_PREFIX.length());
		}
		return token;
	}

	private Claims parseClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(signingKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	private Key signingKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes());
	}

}
