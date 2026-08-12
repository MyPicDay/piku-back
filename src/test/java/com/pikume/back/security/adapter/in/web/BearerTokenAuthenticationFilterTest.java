package com.pikume.back.security.adapter.in.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pikume.back.security.adapter.out.token.JwtTokenProvider;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("BearerTokenAuthenticationFilter")
class BearerTokenAuthenticationFilterTest {

	@Mock private JwtTokenProvider jwtTokenProvider;
	@Mock private QueryUserIdentityUseCase queryUserIdentityUseCase;
	@Mock private AuthenticationEntryPoint authenticationEntryPoint;
	@Mock private FilterChain filterChain;
	private BearerTokenAuthenticationFilter authenticationFilter;

	@BeforeEach
	void setUp() {
		authenticationFilter = new BearerTokenAuthenticationFilter(
				jwtTokenProvider,
				queryUserIdentityUseCase,
				authenticationEntryPoint);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Bearer Token이 없으면 인증하지 않고 다음 필터로 진행한다")
	void requestWithoutBearerTokenContinuesFilterChain() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/diary");
		MockHttpServletResponse response = new MockHttpServletResponse();

		authenticationFilter.doFilter(request, response, filterChain);

		then(filterChain).should().doFilter(request, response);
		verifyNoInteractions(jwtTokenProvider, queryUserIdentityUseCase, authenticationEntryPoint);
	}

	@Test
	@DisplayName("유효하지 않은 JWT는 인증을 거부한다")
	void invalidJwtIsRejected() throws Exception {
		MockHttpServletRequest request = bearerRequest("invalid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

		authenticationFilter.doFilter(request, response, filterChain);

		then(authenticationEntryPoint).should()
				.commence(eq(request), eq(response), any(BadCredentialsException.class));
		then(jwtTokenProvider).should(never()).getUserIdFromToken(any());
		then(filterChain).should(never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("유효한 JWT에 사용자 ID가 없으면 client IP 없이 warn으로 기록한다")
	void validJwtWithoutUserIdLogsWarnWithoutClientIp() throws Exception {
		MockHttpServletRequest request = bearerRequest("refresh-token");
		request.addHeader("X-Forwarded-For", "203.0.113.42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
		given(jwtTokenProvider.getUserIdFromToken("refresh-token"))
				.willThrow(new BadCredentialsException("사용자 ID가 없는 토큰입니다."));

		Logger logger = (Logger) LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			authenticationFilter.doFilter(request, response, filterChain);
		} finally {
			logger.detachAppender(appender);
		}

		assertThat(appender.list)
				.anySatisfy(event -> {
					assertThat(event.getLevel()).isEqualTo(Level.WARN);
					assertThat(event.getFormattedMessage())
							.contains("event=jwt_subject_missing")
							.contains("outcome=denied")
							.contains("reason=missing_user_id")
							.doesNotContain("clientIp")
							.doesNotContain("203.0.113.42");
				});
		then(authenticationEntryPoint).should()
				.commence(any(), any(), any());
		then(filterChain).should(never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("토큰 사용자가 존재하지 않으면 인증을 거부한다")
	void tokenForMissingUserIsRejected() throws Exception {
		MockHttpServletRequest request = bearerRequest("access-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
		given(jwtTokenProvider.getUserIdFromToken("access-token")).willReturn("missing-user");
		given(queryUserIdentityUseCase.queryUserIdentityById("missing-user")).willReturn(Optional.empty());

		authenticationFilter.doFilter(request, response, filterChain);

		then(authenticationEntryPoint).should()
				.commence(eq(request), eq(response), any(BadCredentialsException.class));
		then(filterChain).should(never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("유효한 사용자 JWT는 SecurityContext를 설정하고 다음 필터로 진행한다")
	void validUserJwtAuthenticatesAndContinuesFilterChain() throws Exception {
		MockHttpServletRequest request = bearerRequest("access-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		UserIdentityView user = new UserIdentityView(
				"user-1",
				"password",
				"nickname",
				new UserAvatarReference("avatar.webp", false, false));
		given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
		given(jwtTokenProvider.getUserIdFromToken("access-token")).willReturn("user-1");
		given(queryUserIdentityUseCase.queryUserIdentityById("user-1")).willReturn(Optional.of(user));

		authenticationFilter.doFilter(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
		assertThat(((UserPrincipal) authentication.getPrincipal()).getId()).isEqualTo("user-1");
		then(filterChain).should().doFilter(request, response);
		verifyNoInteractions(authenticationEntryPoint);
	}

	private MockHttpServletRequest bearerRequest(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, AuthWebConstants.BEARER_PREFIX + token);
		return request;
	}
}
