package com.pikume.back.security.config;

import com.pikume.back.admin.adapter.in.web.AdminVisitStatisticsFilter;
import com.pikume.back.security.adapter.in.web.ProblemDetailAccessDeniedHandler;
import com.pikume.back.security.adapter.in.web.ProblemDetailAuthenticationEntryPoint;
import com.pikume.back.security.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final AdminOriginValidationFilter adminOriginValidationFilter;
	private final AdminCsrfValidationFilter adminCsrfValidationFilter;
	private final AdminSessionAuthenticationFilter adminSessionAuthenticationFilter;
	private final AdminVisitStatisticsFilter adminVisitStatisticsFilter;
	private final AdminSecurityProperties adminSecurityProperties;
	private final Environment env;
	private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
	private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

	@Value("${monitoring.allowed-ips:}")
	private String allowedIps;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration adminConfiguration = new CorsConfiguration();
		adminConfiguration.setAllowedOrigins(adminSecurityProperties.allowedOrigins());
		adminConfiguration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		adminConfiguration.setAllowedHeaders(List.of("Content-Type", "Accept", adminSecurityProperties.csrfHeaderName()));
		adminConfiguration.setAllowCredentials(true);

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"
                , "https://pikume.com", "https://www.pikume.com"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/admin/**", adminConfiguration);
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/admin/**")
				.securityContext(context -> context.requireExplicitSave(false))
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/api/admin/**").permitAll()
						.requestMatchers(
								"/api/admin/auth/csrf",
								"/api/admin/auth/temporary-login",
								"/api/admin/auth/login",
								"/api/admin/auth/onboarding/**",
								"/api/admin/auth/otp/verify").permitAll()
						.anyRequest().hasRole("ADMIN"))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(adminOriginValidationFilter, CorsFilter.class);
		http.addFilterAfter(adminCsrfValidationFilter, AdminOriginValidationFilter.class);
		http.addFilterAfter(adminSessionAuthenticationFilter, AdminCsrfValidationFilter.class);
		http.addFilterAfter(adminVisitStatisticsFilter, AdminSessionAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		List<String> permittedPaths = new ArrayList<>(Arrays.asList(
				"/api/auth/login",
				"/api/auth/reissue",
				"/api/auth/signup",
				"/api/auth/send-verification/sign-up",
				"/api/auth/send-verification/password-reset",
				"/api/auth/verify-code",
				"/api/auth/password-reset",
				"/api/auth/email",
				"/api/auth/email-domains",
				"/api/mobile/auth/**",
				"/api/characters/fixed",
				"/api/search"));

		if (Arrays.asList(env.getActiveProfiles()).contains("dev")) {
			permittedPaths.addAll(Arrays.asList(
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-resources/**",
					"/swagger-ui.html"));
		}

		http
				.securityContext(context -> context.requireExplicitSave(false))
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**").access((authentication, context) -> {
							String remoteAddr = context.getRequest().getRemoteAddr();

							if (remoteAddr.equals("127.0.0.1") ||
									remoteAddr.equals("0:0:0:0:0:0:0:1") ||
									remoteAddr.equals("localhost")) {
								return new org.springframework.security.authorization.AuthorizationDecision(true);
							}

							if (allowedIps != null && !allowedIps.isEmpty()) {
								String[] ips = allowedIps.split(",");
								for (String ip : ips) {
									IpAddressMatcher matcher = new IpAddressMatcher(ip.trim());
									if (matcher.matches(context.getRequest())) {
										return new org.springframework.security.authorization.AuthorizationDecision(true);
									}
								}
							}

							return new org.springframework.security.authorization.AuthorizationDecision(false);
						})
						.requestMatchers(permittedPaths.toArray(new String[0]))
						.permitAll()
						.requestMatchers("/api/auth/me").authenticated()
						.requestMatchers("/api/diary/ai/**").authenticated()
						.requestMatchers(HttpMethod.GET,
								"/api/diary",
								"/api/diary/**",
								"/api/comments",
								"/api/comments/*/replies",
								"/api/users/{userId}/profile-preview")
						.permitAll()
						.anyRequest().authenticated())
				.sessionManagement(
						(sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
