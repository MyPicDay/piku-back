package com.pikume.back.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Global technical module boundaries")
class GlobalModuleBoundaryTest {

	private static final Path GLOBAL = Path.of("src/main/java/com/pikume/back/global");
	private static final List<String> CONTEXT_PACKAGES = List.of(
			"com.pikume.back.admin.",
			"com.pikume.back.character.",
			"com.pikume.back.creative.",
			"com.pikume.back.diary.",
			"com.pikume.back.feed.",
			"com.pikume.back.notification.",
			"com.pikume.back.recommendation.",
			"com.pikume.back.social.",
			"com.pikume.back.support.",
			"com.pikume.back.user.",
			"com.pikume.back.security.");
	private static final Map<String, String> STAGED_COMPATIBILITY_DEPENDENCIES = Map.of(
			"src/main/java/com/pikume/back/global/util/FileUtil.java",
			"com.pikume.back.character.domain.vo.CharacterCreationType",
			"src/main/java/com/pikume/back/global/config/OpenApiConfig.java",
			"com.pikume.back.security.config.AdminSecurityProperties");

	@Test
	@DisplayName("Global은 단계적 호환 대상으로 고정한 두 의존 외에 Context 타입을 참조하지 않는다")
	void globalDoesNotAddContextDependenciesBeyondStagedCompatibility() throws IOException {
		List<String> violations = javaSources(GLOBAL)
				.flatMap(path -> CONTEXT_PACKAGES.stream()
						.filter(contextPackage -> contains(path, contextPackage))
						.map(contextPackage -> path + " -> " + contextPackage))
				.filter(violation -> !isStagedCompatibilityDependency(violation))
				.toList();

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("Global의 단계적 호환 의존은 파일과 타입 단위로 고정한다")
	void stagedCompatibilityDependenciesRemainExplicit() {
		assertThat(STAGED_COMPATIBILITY_DEPENDENCIES).containsExactlyInAnyOrderEntriesOf(Map.of(
				"src/main/java/com/pikume/back/global/util/FileUtil.java",
				"com.pikume.back.character.domain.vo.CharacterCreationType",
				"src/main/java/com/pikume/back/global/config/OpenApiConfig.java",
				"com.pikume.back.security.config.AdminSecurityProperties"));
	}

	@Test
	@DisplayName("Global Object Storage Port는 다른 Context 타입을 노출하지 않는다")
	void objectStoragePortsDoNotExposeContextTypes() throws IOException {
		Path ports = GLOBAL.resolve("port/out");

		assertThat(javaSources(ports)
				.filter(path -> CONTEXT_PACKAGES.stream().anyMatch(contextPackage -> contains(path, contextPackage)))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(ports.resolve("ResolveObjectUrlPort.java")).exists();
	}

	private boolean isStagedCompatibilityDependency(String violation) {
		return STAGED_COMPATIBILITY_DEPENDENCIES.entrySet().stream()
				.anyMatch(entry -> violation.startsWith(entry.getKey() + " -> ")
						&& contains(Path.of(entry.getKey()), entry.getValue()));
	}

	private java.util.stream.Stream<Path> javaSources(Path root) throws IOException {
		return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
	}

	private boolean contains(Path path, String fragment) {
		try {
			return Files.readString(path).contains(fragment);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
