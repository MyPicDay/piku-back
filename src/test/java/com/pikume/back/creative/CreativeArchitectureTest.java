package com.pikume.back.creative;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Creative hexagonal architecture")
class CreativeArchitectureTest {

	private static final Path CREATIVE = Path.of("src/main/java/com/pikume/back/creative");
	private static final Path APPLICATION = CREATIVE.resolve("application");
	private static final Path DOMAIN = CREATIVE.resolve("domain");

	@Test
	@DisplayName("Creative Domain은 Application, Adapter, 다른 Context와 Spring 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.creative.application.",
				"com.pikume.back.creative.adapter.",
				"com.pikume.back.admin.",
				"com.pikume.back.character.",
				"com.pikume.back.diary.",
				"com.pikume.back.user.",
				"org.springframework.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Creative Application은 다른 Context, Web, Provider와 Global 기술 타입에 의존하지 않는다")
	void applicationDoesNotDependOnOtherContextsOrTechnicalImplementations() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.creative.adapter.",
				"com.pikume.back.admin.",
				"com.pikume.back.character.",
				"com.pikume.back.diary.",
				"com.pikume.back.user.",
				"com.pikume.back.global.",
				"org.springframework.web.",
				"software.amazon.",
				"com.google.");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("생성 이력과 쿼터 In Port는 명령과 조회 목적별로 분리한다")
	void inboundPortsAreSplitByPurpose() {
		Path inboundPorts = APPLICATION.resolve("port/in");

		for (String port : List.of(
				"QueryDiaryImageGenerationUseCase.java",
				"AttachGeneratedImageToDiaryUseCase.java",
				"UpdateGeneratedImagePathUseCase.java",
				"QueryAiGenerationQuotaUseCase.java",
				"ConsumeAiGenerationQuotaUseCase.java",
				"PrepareCharacterReferenceUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
		assertThat(inboundPorts.resolve("ManageGenerationUseCase.java")).doesNotExist();
		assertThat(inboundPorts.resolve("ManageAiGenerationQuotaUseCase.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/LoadGenerationPort.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/LoadGenerationForDiaryPort.java")).exists();
		assertThat(APPLICATION.resolve("port/out/LoadGenerationStatisticsPort.java")).exists();
	}

	@Test
	@DisplayName("Creative 생성 이미지 저장은 Creative Adapter가 소유하고 Diary는 Creative Port를 구현하지 않는다")
	void creativeOwnsGeneratedImageStorageAdapter() {
		assertThat(CREATIVE.resolve("adapter/out/storage/CreativeImageStorageAdapter.java")).exists();
		assertThat(CREATIVE.resolve("adapter/out/storage/CreativeImageObjectKeyPolicy.java")).exists();
		assertThat(Path.of(
				"src/main/java/com/pikume/back/diary/adapter/out/storage/SharedImageStorageCompatibilityAdapter.java"))
				.doesNotExist();
	}

	@Test
	@DisplayName("Creative Cross-context Adapter는 Provider Domain과 Global 캐릭터 정책을 참조하지 않는다")
	void crossContextAdaptersUsePublishedContractsAndSingleExternalBoundaries() throws IOException {
		Path crossContext = CREATIVE.resolve("adapter/out/crosscontext");

		assertThat(crossContext.resolve("CharacterReferenceAdapter.java")).doesNotExist();
		assertThat(crossContext.resolve("CharacterReferenceAdapterForCreative.java")).exists();
		assertThat(crossContext.resolve("UserAvatarReferenceAdapter.java")).exists();
		assertThat(CREATIVE.resolve("adapter/out/storage/ReferenceImageObjectAdapter.java")).exists();
		assertThat(javaSources(crossContext)
				.filter(path -> contains(path, "com.pikume.back.admin.domain.")
						|| contains(path, "com.pikume.back.character.domain.")
						|| contains(path, "com.pikume.back.character.application.service.")
						|| contains(path, "com.pikume.back.character.application.port.out.")
						|| contains(path, "com.pikume.back.character.adapter.")
						|| contains(path, "com.pikume.back.global.util.CharacterAvatarPathNormalizer"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Creative Web Adapter는 Application 오류를 직접 응답으로 만들지 않는다")
	void webAdapterDelegatesProblemDetailsToExceptionHandler() {
		Path controller = CREATIVE.resolve("adapter/in/web/AiGeneratorController.java");

		assertThat(contains(controller, "ProblemDetailFactory")).isFalse();
		assertThat(contains(controller, "catch (")).isFalse();
		assertThat(contains(controller, "ProblemDetail.class")).isTrue();
		assertThat(CREATIVE.resolve("adapter/in/web/CreativeExceptionHandler.java")).exists();
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
