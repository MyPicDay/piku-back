package com.pikume.back.diary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diary hexagonal architecture")
class DiaryArchitectureTest {

	private static final Path DIARY_APPLICATION = Path.of("src/main/java/com/pikume/back/diary/application");

	@Test
	@DisplayName("Diary Application은 다른 Context와 Spring 트랜잭션 동기화 API를 직접 사용하지 않는다")
	void applicationDoesNotDependOnOtherContextsOrTransactionSynchronization() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.global.",
				"com.pikume.back.creative.",
				"com.pikume.back.notification.",
				"com.pikume.back.recommendation.",
				"com.pikume.back.social.",
				"com.pikume.back.user.",
				"org.springframework.transaction.support.",
				"com.pikume.back.global.util.FileUtil");

		assertThat(javaSources(DIARY_APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(javaSources(DIARY_APPLICATION.resolve("service"))
				.filter(path -> contains(path, "LoadDiaryPort") || contains(path, "SaveDiaryPort"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Diary 명령과 조회 서비스는 유스케이스 목적별 클래스로 분리한다")
	void servicesAreSplitByUseCasePurpose() {
		Path services = DIARY_APPLICATION.resolve("service");

		for (String service : List.of(
				"DiaryCreationService.java",
				"DiaryUpdateService.java",
				"DiaryDeletionService.java",
				"DiaryPhotoRelocationService.java",
				"DiaryDetailQueryService.java",
				"DiaryFeedQueryService.java",
				"DiaryCalendarQueryService.java",
				"DiaryGalleryQueryService.java",
				"DiaryReadQueryService.java")) {
			assertThat(services.resolve(service)).exists();
		}

		assertThat(services.resolve("DiaryCommandService.java")).doesNotExist();
		assertThat(services.resolve("DiaryQueryService.java")).doesNotExist();
	}

	@Test
	@DisplayName("Diary Cross-context Adapter는 표준 폴더에만 둔다")
	void crossContextAdaptersUseStandardFolder() throws IOException {
		Path legacyFolder = Path.of("src/main/java/com/pikume/back/diary/adapter/out/friend");
		assertThat(Files.exists(legacyFolder) && javaSources(legacyFolder).findAny().isPresent()).isFalse();
	}

	@Test
	@DisplayName("Diary MinIO Adapter는 Diary가 소유한 Port만 구현한다")
	void minioPhotoStorageAdapterImplementsOnlyDiaryPorts() {
		Path adapter = Path.of("src/main/java/com/pikume/back/diary/adapter/out/storage/MinioPhotoStorageAdapter.java");

		assertThat(contains(adapter, "com.pikume.back.creative.application.port.out")).isFalse();
		assertThat(contains(adapter, "com.pikume.back.global.port.out")).isFalse();
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
