package com.pikume.back.security.jwt;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import com.pikume.back.user.auth.constants.AuthConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final LoadUserForAuthPort loadUserForAuthPort;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
			chain.doFilter(request, response);
			return;
		}
		String token = authHeader.substring(AuthConstants.BEARER_PREFIX.length());
		try {
			if (!jwtProvider.validateToken(token) || jwtProvider.getTokenType(token) != SecurityTokenType.USER_ACCESS) {
				throw new BadCredentialsException("인증이 필요합니다.");
			}
			authenticateUser(token);
			chain.doFilter(request, response);
		} catch (Exception exception) {
			log.warn("event=jwt_filter_authentication_failed outcome=denied reason={}",
					exception.getClass().getSimpleName());
			authenticationEntryPoint.commence(request, response, new BadCredentialsException("인증이 필요합니다."));
		}
	}

	private void authenticateUser(String token) {
		String userId = jwtProvider.getUserIdFromToken(token);
		AuthUserView user = loadUserForAuthPort.findById(userId)
				.orElseThrow(() -> new BadCredentialsException("인증이 필요합니다."));
		CustomUserDetails userDetails = CustomUserDetails.withAvatarPath(
				user.id(), user.nickname(), user.avatarPath());
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
	}
}
