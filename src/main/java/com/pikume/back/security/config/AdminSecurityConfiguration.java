package com.pikume.back.security.config;

import com.pikume.back.security.adapter.in.web.ProblemDetailAccessDeniedHandler;
import com.pikume.back.security.adapter.in.web.ProblemDetailAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfiguration {

	private final AdminOriginValidationFilter adminOriginValidationFilter;
	private final AdminCsrfValidationFilter adminCsrfValidationFilter;
	private final AdminSessionAuthenticationFilter adminSessionAuthenticationFilter;
	private final AdminSecurityChainExtension adminSecurityChainExtension;
	private final CorsConfigurationSource corsConfigurationSource;
	private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
	private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

	@Bean
	@Order(1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/admin/**")
				.securityContext(context -> context.requireExplicitSave(false))
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
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
		http.addFilterAfter(adminSecurityChainExtension, AdminSessionAuthenticationFilter.class);
		return http.build();
	}
}
