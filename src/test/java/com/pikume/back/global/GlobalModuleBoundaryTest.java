package com.pikume.back.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

	@Test
	@DisplayName("Global은 어떤 Context나 Security 구현 타입도 참조하지 않는다")
	void globalDoesNotDependOnContextsOrSecurityImplementations() throws IOException {
		List<String> violations = javaSources(GLOBAL)
				.flatMap(path -> CONTEXT_PACKAGES.stream()
						.filter(contextPackage -> contains(path, contextPackage))
						.map(contextPackage -> path + " -> " + contextPackage))
				.toList();

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("도메인 전용 Global 파일 및 호환 이미지 Port를 제거한다")
	void legacyDomainSpecificGlobalFilesAreRemoved() {
		assertThat(GLOBAL.resolve("util/FileUtil.java")).doesNotExist();
		assertThat(GLOBAL.resolve("util/FileConstants.java")).doesNotExist();
		assertThat(GLOBAL.resolve("util/CharacterAvatarPathNormalizer.java")).doesNotExist();
		assertThat(GLOBAL.resolve("util/ImagePathToUrlConverter.java")).doesNotExist();
		assertThat(GLOBAL.resolve("port/out/ResolveImageUrlPort.java")).doesNotExist();
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

	@Test
	@DisplayName("중립 Object Storage Adapter가 Global 기술 Port를 구현한다")
	void neutralObjectStorageAdapterImplementsGlobalPorts() {
		Path adapter = GLOBAL.resolve("storage/S3ObjectStorageAdapter.java");

		assertThat(adapter).exists();
		assertThat(contains(adapter, "LoadObjectPort")).isTrue();
		assertThat(contains(adapter, "StoreObjectPort")).isTrue();
		assertThat(contains(adapter, "ResolveObjectUrlPort")).isTrue();
		assertThat(Path.of(
				"src/main/java/com/pikume/back/diary/adapter/out/storage/SharedImageStorageCompatibilityAdapter.java"))
				.doesNotExist();
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
