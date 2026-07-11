package com.pikume.back.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Architecture boundaries")
class ArchitectureBoundaryTest {

	@Test
	@DisplayName("security outbound adapters는 user persistence adapters 또는 user domain entities에 대한 의존성을 갖지 않는다.")
	void securityOutboundAdaptersDoNotDependOnUserPersistenceOrDomain() throws IOException {
		Path securityOutboundAdapters = Path.of("src/main/java/com/pikume/back/security/adapter/out");
		List<String> violations = findJavaSourceViolations(
				securityOutboundAdapters,
				this::importsUserPersistenceOrDomain);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("creative application은 웹 보안 principal에 의존하지 않는다.")
	void creativeApplicationDoesNotDependOnWebSecurityPrincipal() throws IOException {
		Path creativeApplication = Path.of("src/main/java/com/pikume/back/creative/application");

		assertThat(findJavaSourceViolations(
				creativeApplication,
				path -> sourceContains(path, "com.pikume.back.global.config.CustomUserDetails")))
				.isEmpty();
	}

	@Test
	@DisplayName("creative outbound cross-context adapter는 crosscontext 폴더에 위치한다.")
	void creativeCrossContextAdaptersUseStandardFolder() throws IOException {
		Path creativeOutboundAdapters = Path.of("src/main/java/com/pikume/back/creative/adapter/out");

		assertThat(findJavaSourceViolations(
				creativeOutboundAdapters,
				path -> path.toString().contains("/adapter/out/user/")))
				.isEmpty();
	}

	@Test
	@DisplayName("diary outbound ports는 creative 도메인의 타입을 노출하지 않는다.")
	void diaryOutboundPortsDoNotExposeCreativeTypes() throws IOException {
		Path diaryOutboundPorts = Path.of("src/main/java/com/pikume/back/diary/application/port/out");

		assertThat(findJavaSourceViolations(
				diaryOutboundPorts,
				path -> sourceContains(path, "import com.pikume.back.creative.")))
				.isEmpty();
	}

	@Test
	@DisplayName("creative ports는 저장소 관용 메서드명을 노출하지 않는다.")
	void creativePortsDoNotExposeRepositoryMethodNames() throws IOException {
		Path creativePorts = Path.of("src/main/java/com/pikume/back/creative/application/port");
		List<String> repositoryMethodNames = List.of(
				" findById(",
				" findByDiaryIdIsNull(",
				" findByUserIdAndFilePath(",
				" existsByIdAndUserId(",
				" save(",
				" saveAIPhoto(");

		assertThat(findJavaSourceViolations(
				creativePorts,
				path -> repositoryMethodNames.stream().anyMatch(name -> sourceContains(path, name))))
				.isEmpty();
	}

	@Test
	@DisplayName("프로덕션 코드는 사용하지 않는 HTTP 요청 메타데이터 계약에 의존하지 않는다.")
	void productionCodeDoesNotDependOnUnusedRequestMetadata() throws IOException {
		Path productionSources = Path.of("src/main/java");

		assertThat(findJavaSourceViolations(
				productionSources,
				path -> sourceContains(path, "RequestMetaInfo")
						|| sourceContains(path, "RequestMetaMapper")))
				.isEmpty();
	}

	private List<String> findJavaSourceViolations(Path root, Predicate<Path> violationPredicate) throws IOException {
		try (var paths = Files.walk(root)) {
			return paths
					.filter(path -> path.toString().endsWith(".java"))
					.filter(violationPredicate)
					.map(Path::toString)
					.toList();
		}
	}

	private boolean importsUserPersistenceOrDomain(Path path) {
		return sourceContains(path, "com.pikume.back.user.adapter.out.persistence")
				|| sourceContains(path, "com.pikume.back.user.domain.User");
	}

	private boolean sourceContains(Path path, String text) {
		try {
			return Files.readString(path).contains(text);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + path, e);
		}
	}
}
