package com.pikume.back.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security technical module architecture")
class SecurityArchitectureTest {

	private static final Path SECURITY = Path.of("src/main/java/com/pikume/back/security");
	private static final Path PRODUCTION = Path.of("src/main/java/com/pikume/back");

	@Test
	@DisplayName("Security는 Domain과 Application 계층을 소유하지 않는다")
	void securityHasOnlyTechnicalAdapterLayers() {
		assertThat(SECURITY.resolve("domain")).doesNotExist();
		assertThat(SECURITY.resolve("application")).doesNotExist();
	}

	@Test
	@DisplayName("일반 사용자와 관리자 Principal은 Security가 소유한다")
	void securityOwnsPrincipals() {
		assertThat(SECURITY.resolve("principal/UserPrincipal.java")).exists();
		assertThat(SECURITY.resolve("principal/AdminPrincipal.java")).exists();
		assertThat(PRODUCTION.resolve("global/config/CustomUserDetails.java")).doesNotExist();
		assertThat(SECURITY.resolve("config/AdminUserDetails.java")).doesNotExist();
	}

	@Test
	@DisplayName("JWT Filter와 Provider는 입력 Web·출력 Token Adapter에 배치한다")
	void jwtComponentsUseAdapterLocations() {
		assertThat(SECURITY.resolve("adapter/in/web/BearerTokenAuthenticationFilter.java")).exists();
		assertThat(SECURITY.resolve("adapter/out/token/JwtTokenProvider.java")).exists();
		assertThat(SECURITY.resolve("jwt/JwtFilter.java")).doesNotExist();
		assertThat(SECURITY.resolve("jwt/JwtProvider.java")).doesNotExist();
	}

	@Test
	@DisplayName("관리자, 일반 사용자와 CORS Filter Chain 구성을 책임별로 분리한다")
	void filterChainsUsePurposeSpecificConfigurations() {
		assertThat(SECURITY.resolve("config/AdminSecurityConfiguration.java")).exists();
		assertThat(SECURITY.resolve("config/UserSecurityConfiguration.java")).exists();
		assertThat(SECURITY.resolve("config/SecurityCorsConfiguration.java")).exists();
		assertThat(SECURITY.resolve("config/SecurityBeanConfiguration.java")).exists();
		assertThat(SECURITY.resolve("config/SecurityConfig.java")).doesNotExist();
	}

	@Test
	@DisplayName("관리자와 일반 사용자 Chain 순서 및 관리자 Filter 선후관계를 유지한다")
	void filterChainsKeepCurrentOrderAndAdminFilterSequence() throws Exception {
		Method adminChain = com.pikume.back.security.config.AdminSecurityConfiguration.class
				.getDeclaredMethod("adminSecurityFilterChain", HttpSecurity.class);
		Method userChain = com.pikume.back.security.config.UserSecurityConfiguration.class
				.getDeclaredMethod("userSecurityFilterChain", HttpSecurity.class);
		assertThat(adminChain.getAnnotation(Order.class).value()).isEqualTo(1);
		assertThat(userChain.getAnnotation(Order.class).value()).isEqualTo(2);

		String source = Files.readString(SECURITY.resolve("config/AdminSecurityConfiguration.java"));
		assertThat(source).contains(
				"addFilterBefore(adminOriginValidationFilter, CorsFilter.class)",
				"addFilterAfter(adminCsrfValidationFilter, AdminOriginValidationFilter.class)",
				"addFilterAfter(adminSessionAuthenticationFilter, AdminCsrfValidationFilter.class)",
				"addFilterAfter(adminSecurityChainExtension, AdminSessionAuthenticationFilter.class)");
		assertThat(source.indexOf("addFilterBefore(adminOriginValidationFilter, CorsFilter.class)"))
				.isLessThan(source.indexOf(
						"addFilterAfter(adminCsrfValidationFilter, AdminOriginValidationFilter.class)"));
		assertThat(source.indexOf(
				"addFilterAfter(adminCsrfValidationFilter, AdminOriginValidationFilter.class)"))
				.isLessThan(source.indexOf(
						"addFilterAfter(adminSessionAuthenticationFilter, AdminCsrfValidationFilter.class)"));
		assertThat(source.indexOf(
				"addFilterAfter(adminSessionAuthenticationFilter, AdminCsrfValidationFilter.class)"))
				.isLessThan(source.indexOf(
						"addFilterAfter(adminSecurityChainExtension, AdminSessionAuthenticationFilter.class)"));
	}

	@Test
	@DisplayName("Filter 기반 오류는 하나의 Security Problem writer를 사용한다")
	void filterErrorsUseOneProblemWriter() throws IOException {
		Path writer = SECURITY.resolve("adapter/in/web/SecurityProblemResponseWriter.java");
		assertThat(writer).exists();
		assertThat(SECURITY.resolve("config/AdminProblemResponseWriter.java")).doesNotExist();

		for (String consumer : List.of(
				"adapter/in/web/ProblemDetailAuthenticationEntryPoint.java",
				"adapter/in/web/ProblemDetailAccessDeniedHandler.java",
				"config/AdminCsrfValidationFilter.java",
				"config/AdminOriginValidationFilter.java",
				"config/AdminSessionAuthenticationFilter.java")) {
			assertThat(contains(SECURITY.resolve(consumer), "SecurityProblemResponseWriter")).isTrue();
		}
		assertThat(javaSources(SECURITY.resolve("adapter/in/web"))
				.filter(path -> !path.equals(writer))
				.filter(path -> contains(path, "ObjectMapper"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Security는 Admin과 User의 내부 Domain·Out Adapter를 참조하지 않는다")
	void securityUsesOnlyPublishedContextContracts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.admin.domain.",
				"com.pikume.back.admin.application.port.out.",
				"com.pikume.back.admin.application.service.",
				"com.pikume.back.admin.adapter.",
				"com.pikume.back.user.domain.",
				"com.pikume.back.user.adapter.");

		assertThat(javaSources(SECURITY)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("User Auth Out Port 구현 의존은 Security 출력 Adapter에만 존재한다")
	void userAuthOutboundContractsAreImplementedOnlyByOutboundAdapters() throws IOException {
		assertThat(javaSources(SECURITY)
				.filter(path -> contains(path, "com.pikume.back.user.auth.application.port.out."))
				.filter(path -> !path.toString().contains("/security/adapter/out/"))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(SECURITY.resolve("adapter/out/persistence/RefreshSessionEntity.java")).exists();
	}

	@Test
	@DisplayName("Security Principal은 다른 Context의 Web Adapter에만 노출된다")
	void principalsAreLimitedToWebAdapters() throws IOException {
		assertThat(javaSources(PRODUCTION)
				.filter(path -> !path.toString().contains("/security/"))
				.filter(path -> contains(path, "com.pikume.back.security.principal."))
				.filter(path -> !path.toString().contains("/adapter/in/web/"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	private Stream<Path> javaSources(Path root) throws IOException {
		return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
	}

	private boolean contains(Path path, String fragment) {
		try {
			return Files.readString(path).contains(fragment);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
