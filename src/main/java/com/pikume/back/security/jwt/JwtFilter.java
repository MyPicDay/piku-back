package com.pikume.back.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.domain.AdminSession;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.security.config.AdminUserDetails;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private static final String ADMIN_PATH_PREFIX = "/api/admin";

	private final JwtProvider jwtProvider;
	private final LoadUserForAuthPort loadUserForAuthPort;
	private final LoadAdminSessionPort loadAdminSessionPort;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (authHeader != null && authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
			String token = authHeader.substring(AuthConstants.BEARER_PREFIX.length());

			try {
				if (!jwtProvider.validateToken(token)) {
					log.warn("event=jwt_filter_authentication_failed outcome=denied reason=invalid_token");
					authenticationEntryPoint.commence(request, response,
							new BadCredentialsException("인증이 필요합니다."));
					return;
				}

				SecurityTokenType tokenType = jwtProvider.getTokenType(token);
				String requestPath = request.getRequestURI();
				if (isAdminHandshakePath(requestPath)) {
					chain.doFilter(request, response);
					return;
				}

				if (isAdminPath(requestPath)) {
					if (tokenType != SecurityTokenType.ADMIN_ACCESS) {
						log.warn("event=jwt_filter_authentication_failed outcome=denied reason=admin_token_required");
						authenticationEntryPoint.commence(request, response,
								new BadCredentialsException("관리자 인증이 필요합니다."));
						return;
					}
					authenticateAdmin(token);
				} else {
					if (tokenType.isAdminScoped() || tokenType != SecurityTokenType.USER_ACCESS) {
						log.warn("event=jwt_filter_authentication_failed outcome=denied reason=user_token_required");
						authenticationEntryPoint.commence(request, response,
								new BadCredentialsException("인증이 필요합니다."));
						return;
					}
					authenticateUser(token);
				}
			} catch (Exception e) {
				log.warn("event=jwt_filter_authentication_failed outcome=denied reason={}",
						e.getClass().getSimpleName());
				authenticationEntryPoint.commence(request, response,
						new BadCredentialsException("인증이 필요합니다."));
				return;
			}
		}

		chain.doFilter(request, response);
	}

	private void authenticateUser(String token) {
		String userId = jwtProvider.getUserIdFromToken(token);
		log.debug("event=jwt_filter_token_validated userId={}", userId);

		AuthUserView user = loadUserForAuthPort.findById(userId)
				.orElseThrow(() -> {
					log.warn("event=jwt_filter_user_lookup_failed userId={}", userId);
					return new RuntimeException("유저 없음");
				});

		CustomUserDetails userDetails = CustomUserDetails.withAvatarPath(
				user.id(), user.nickname(), user.avatarPath());

		Authentication authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);
		log.debug("event=jwt_filter_authentication_set userId={}", user.id());
	}

	private void authenticateAdmin(String token) {
		String adminId = jwtProvider.getUserIdFromToken(token);
		String role = jwtProvider.getAdminRoleFromToken(token);
		String sessionId = jwtProvider.getAdminSessionIdFromToken(token);
		log.debug("event=jwt_filter_admin_token_validated adminId={}", adminId);

		AdminSession session = loadAdminSessionPort.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("관리자 세션 없음"));
		if (!session.belongsTo(adminId) || !session.isActiveAt(LocalDateTime.now())) {
			throw new RuntimeException("관리자 세션이 유효하지 않습니다.");
		}

		AdminUserDetails adminUserDetails = new AdminUserDetails(adminId, role, sessionId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				adminUserDetails, null, adminUserDetails.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);
		log.debug("event=jwt_filter_admin_authentication_set adminId={}", adminId);
	}

	private boolean isAdminPath(String requestPath) {
		return requestPath != null && requestPath.startsWith(ADMIN_PATH_PREFIX);
	}

	private boolean isAdminHandshakePath(String requestPath) {
		return requestPath != null
				&& (requestPath.equals("/api/admin/auth/temporary-login")
				|| requestPath.equals("/api/admin/auth/login")
				|| requestPath.equals("/api/admin/auth/reissue")
				|| requestPath.equals("/api/admin/auth/otp/verify")
				|| requestPath.equals("/api/admin/accounts/email-change/confirm")
				|| requestPath.startsWith("/api/admin/auth/onboarding/")
				|| requestPath.startsWith("/api/admin/auth/password-reset/"));
	}
}
