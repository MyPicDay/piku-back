package com.pikume.back.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Admin hexagonal architecture")
class AdminArchitectureTest {

	private static final Path ADMIN = Path.of("src/main/java/com/pikume/back/admin");
	private static final Path APPLICATION = ADMIN.resolve("application");
	private static final Path DOMAIN = ADMIN.resolve("domain");
	private static final Path SECURITY = Path.of("src/main/java/com/pikume/back/security");

	@Test
	@DisplayName("Admin Domain은 Application, Adapter, 다른 Context와 Spring 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.admin.application.",
				"com.pikume.back.admin.adapter.",
				"com.pikume.back.user.",
				"com.pikume.back.diary.",
				"com.pikume.back.creative.",
				"org.springframework.",
				"jakarta.servlet.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Admin Application은 HTTP, Security 구현과 다른 Context에 의존하지 않는다")
	void applicationUsesTechnicalNeutralContracts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.admin.adapter.",
				"com.pikume.back.security.",
				"com.pikume.back.user.",
				"com.pikume.back.diary.",
				"com.pikume.back.creative.",
				"com.pikume.back.global.error.",
				"org.springframework.http.",
				"org.springframework.security.",
				"jakarta.servlet.");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Admin 오류와 Security 공개 Result는 Application 소유 기술 중립 계약이다")
	void exposesNeutralAdminErrorsAndSecurityResults() {
		assertThat(APPLICATION.resolve("exception/AdminErrorCode.java")).exists();
		assertThat(APPLICATION.resolve("exception/AdminProblem.java")).doesNotExist();
		assertThat(APPLICATION.resolve("dto/AdminSessionCredentialResult.java")).exists();
		assertThat(APPLICATION.resolve("dto/AuthenticatedAdminSessionResult.java")).exists();
		assertThat(APPLICATION.resolve("dto/AdminDailyCount.java")).exists();
		assertThat(APPLICATION.resolve("service/AdminSessionCredentialResult.java")).doesNotExist();
		assertThat(APPLICATION.resolve("service/AuthenticatedAdminSessionResult.java")).doesNotExist();
		assertThat(APPLICATION.resolve("service/AdminDailyCount.java")).doesNotExist();
		assertThat(ADMIN.resolve("adapter/in/web/problem/AdminProblemType.java")).exists();
	}

	@Test
	@DisplayName("세션 생명주기와 Security 관측 사건은 Admin In Port로 표현한다")
	void securityCollaborationUsesAdminInboundPorts() {
		assertThat(APPLICATION.resolve("port/in/ManageAdminSessionLifecycleUseCase.java")).exists();
		assertThat(APPLICATION.resolve("port/in/RecordAdminSecurityEventUseCase.java")).exists();
		assertThat(APPLICATION.resolve("port/out/ManageAdminSessionLifecycleUseCase.java")).doesNotExist();
	}

	@Test
	@DisplayName("Admin Persistence Port는 Load, Save와 Repository 관용 이름을 노출하지 않는다")
	void persistencePortsUseDomainIntentNames() throws IOException {
		Path outboundPorts = APPLICATION.resolve("port/out");
		List<String> obsoleteNames = List.of(
				"LoadAdminAccountPort.java",
				"SaveAdminAccountPort.java",
				"LoadAdminSessionPort.java",
				"SaveAdminSessionPort.java",
				"LoadAdminAuditLogPort.java",
				"SaveAdminAuditLogPort.java",
				"LoadAdminDailyStatisticsPort.java",
				"SaveAdminDailyStatisticsPort.java",
				"SaveAdminStatisticsEventPort.java",
				"SaveAdminCredentialsPort.java");

		for (String obsoleteName : obsoleteNames) {
			assertThat(outboundPorts.resolve(obsoleteName)).doesNotExist();
		}

		List<String> repositoryMethodNames = List.of(
				" findById(",
				" findByEmail(",
				" findByLoginId(",
				" findBySessionTokenHash(",
				" findByDateBetween(",
				" findLatest(",
				" save(");
		assertThat(javaSources(outboundPorts)
				.filter(path -> repositoryMethodNames.stream().anyMatch(name -> contains(path, name)))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(javaSources(outboundPorts)
				.filter(path -> contains(path, "com.pikume.back.admin.application.service."))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Admin Cross-context Adapter는 Provider 공개 In Port만 사용한다")
	void crossContextAdaptersUseProviderPublishedContracts() throws IOException {
		Path crossContext = ADMIN.resolve("adapter/out/crosscontext");
		List<String> forbiddenDependencies = List.of(
				".application.port.out.",
				".adapter.",
				".domain.");

		assertThat(javaSources(crossContext)
				.filter(path -> Stream.of("com.pikume.back.user.", "com.pikume.back.diary.",
								"com.pikume.back.creative.")
						.anyMatch(context -> forbiddenDependencies.stream()
								.anyMatch(layer -> contains(path, context + layer.substring(1)))))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Security는 Admin Domain, Out Port, Service 내부 Result와 Web Adapter에 의존하지 않는다")
	void securityUsesOnlyAdminPublishedInboundContracts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.admin.domain.",
				"com.pikume.back.admin.application.port.out.",
				"com.pikume.back.admin.application.service.",
				"com.pikume.back.admin.adapter.in.web.");

		assertThat(javaSources(SECURITY)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
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
