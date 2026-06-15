package com.pikume.back.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.security.config.CustomUserDetailService;
import com.pikume.back.global.config.CustomUserDetails;

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
	 */
	public String generateAccessToken(String userId) {
		log.debug("event=access_token_generation_requested userId={}", userId);

		Claims claims = Jwts.claims().setSubject(userId);
		Date now = new Date();
		Date expiry = new Date(now.getTime() + AuthConstants.ACCESS_TOKEN_EXPIRATION_TIME);
		Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

		claims.put("roles", List.of("ROLE_USER"));

		log.debug("event=access_token_generated userId={} expiresAt={}", userId, expiry);

		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(key)
				.compact();
	}

	/*
	 * JWT Refresh Token 생성
	 */
	public String generateRefreshToken() {
		log.debug("event=refresh_token_generation_requested");

		Date now = new Date();
		Date expiry = new Date(now.getTime() + AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME);
		Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

		log.debug("event=refresh_token_generated expiresAt={}", expiry);

		return Jwts.builder()
				.setExpiration(expiry)
				.signWith(key)
				.compact();
	}

	/*
	 * JWT에서 사용자 ID 추출
	 */
	public String getUserIdFromToken(String token) {
		token = cleanToken(token);
		log.debug("event=jwt_subject_parse_requested");

		Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

		String userId = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();

		log.debug("event=jwt_subject_parsed userId={}", userId);
		return userId;
	}

	/*
	 * 토큰 유효성 검사
	 */
	public boolean validateToken(String token) {
		try {
			token = cleanToken(token);
			Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

			Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token);
			log.debug("event=jwt_validation_succeeded");
			return true;

		} catch (Exception e) {
			log.warn("event=jwt_validation_failed reason={}", e.getClass().getSimpleName());
			return false;
		}
	}

	public String cleanToken(String token) {
		if (token != null && token.startsWith(AuthConstants.BEARER_PREFIX)) {
			return token.substring(AuthConstants.BEARER_PREFIX.length());
		}
		return token;
	}

	public Authentication getAuthentication(String token) {
		token = cleanToken(token);
		String userId = getUserIdFromToken(token);

		CustomUserDetails userDetails = (CustomUserDetails) customUserDetailService.loadUserByUsername(userId);

		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}
}
