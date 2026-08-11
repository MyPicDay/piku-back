package com.pikume.back.character;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Character hexagonal architecture")
class CharacterArchitectureTest {

	private static final Path CHARACTER = Path.of("src/main/java/com/pikume/back/character");
	private static final Path APPLICATION = CHARACTER.resolve("application");
	private static final Path DOMAIN = CHARACTER.resolve("domain");

	@Test
	@DisplayName("Character Domain은 Application, Adapter와 다른 Context에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.character.application.",
				"com.pikume.back.character.adapter.",
				"com.pikume.back.creative.",
				"com.pikume.back.diary.",
				"com.pikume.back.user.",
				"org.springframework.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Character Application은 다른 Context, Global 이미지 정책과 Adapter 구현에 의존하지 않는다")
	void applicationDoesNotDependOnOtherContextsOrImageInfrastructure() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.character.adapter.",
				"com.pikume.back.creative.",
				"com.pikume.back.diary.",
				"com.pikume.back.user.",
				"com.pikume.back.global.port.out.ResolveImageUrlPort",
				"com.pikume.back.global.util.CharacterAvatarPathNormalizer",
				"com.pikume.back.global.util.FileUtil");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Character 유스케이스는 카탈로그 조회, 참조 해석과 동기화 목적별로 분리한다")
	void inboundPortsAreSplitByPurpose() {
		Path inboundPorts = APPLICATION.resolve("port/in");

		for (String port : List.of(
				"QueryFixedCharacterCatalogUseCase.java",
				"QueryCharacterImageReferencesUseCase.java",
				"ResolveFixedCharacterReferenceUseCase.java",
				"SynchronizeFixedCharacterCatalogUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
		assertThat(inboundPorts.resolve("ManageCharacterUseCase.java")).doesNotExist();
	}

	@Test
	@DisplayName("Character Application Service는 카탈로그 조회, 참조 해석과 동기화로 분리한다")
	void servicesAreSplitByPurpose() {
		Path services = APPLICATION.resolve("service");

		for (String service : List.of(
				"FixedCharacterCatalogQueryService.java",
				"CharacterImageReferenceQueryService.java",
				"FixedCharacterReferenceService.java",
				"FixedCharacterCatalogSynchronizationService.java")) {
			assertThat(services.resolve(service)).exists();
		}
		assertThat(services.resolve("CharacterService.java")).doesNotExist();
	}

	@Test
	@DisplayName("사용되지 않는 AI 캐릭터 로컬 저장 계약은 Character에서 제거한다")
	void unusedLocalCharacterImageStorageIsRemoved() {
		assertThat(APPLICATION.resolve("port/out/CharacterImageStoragePort.java")).doesNotExist();
		assertThat(CHARACTER.resolve("adapter/out/storage/CharacterImageStorageAdapter.java")).doesNotExist();
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
