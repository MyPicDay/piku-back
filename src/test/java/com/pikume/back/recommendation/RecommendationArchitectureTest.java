package com.pikume.back.recommendation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recommendation hexagonal architecture")
class RecommendationArchitectureTest {

	private static final Path RECOMMENDATION = Path.of("src/main/java/com/pikume/back/recommendation");
	private static final Path APPLICATION = RECOMMENDATION.resolve("application");
	private static final Path DOMAIN = RECOMMENDATION.resolve("domain");

	@Test
	@DisplayName("Recommendation Domain은 Application, Adapter, 다른 Context와 기술 직렬화에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.recommendation.application.",
				"com.pikume.back.recommendation.adapter.",
				"com.pikume.back.diary.",
				"com.pikume.back.feed.",
				"com.fasterxml.jackson.",
				"org.springframework.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Recommendation Application은 다른 Context, Jackson과 Adapter 구현에 의존하지 않는다")
	void applicationDoesNotDependOnOtherContextsOrTechnicalImplementations() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.recommendation.adapter.",
				"com.pikume.back.diary.",
				"com.pikume.back.feed.",
				"com.fasterxml.jackson.",
				"org.springframework.web.",
				"org.springframework.data.");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("분석, 메타데이터 조회, 선호도 기록과 후보 점수 계약은 목적별로 분리한다")
	void applicationPortsAreSplitByPurpose() {
		Path inboundPorts = APPLICATION.resolve("port/in");

		for (String port : List.of(
				"AnalyzeDiaryContentUseCase.java",
				"QueryDiaryMetadataUseCase.java",
				"RecordTopicInteractionUseCase.java",
				"QueryUserTopicAffinitiesUseCase.java",
				"ScoreDiaryCandidatesUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
		assertThat(inboundPorts.resolve("ManageUserPreferenceUseCase.java")).doesNotExist();
		assertThat(inboundPorts.resolve("GetRecommendationUseCase.java")).doesNotExist();
	}

	@Test
	@DisplayName("콘텐츠 분석과 현재 시각은 기술 중립 Out Port 뒤에 둔다")
	void analyzerAndClockAreTechnicalBoundaries() {
		Path outboundPorts = APPLICATION.resolve("port/out");
		Path analyzer = RECOMMENDATION.resolve("adapter/out/analyzer/LocalContentAnalyzerAdapter.java");

		assertThat(outboundPorts.resolve("RecommendationClockPort.java")).exists();
		assertThat(contains(analyzer, "DiaryMetadata")).isFalse();
		assertThat(RECOMMENDATION.resolve("adapter/out/time/SystemRecommendationClockAdapter.java")).exists();
	}

	@Test
	@DisplayName("운영 호출이 없는 Recommendation 소유 Feed 캐시와 레거시 점수 타입은 제거한다")
	void unusedFeedCacheAndLegacyScoreAreRemoved() {
		assertThat(APPLICATION.resolve("port/in/CacheFeedUseCase.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/RecommendationCachePort.java")).doesNotExist();
		assertThat(APPLICATION.resolve("service/RecommendationCacheService.java")).doesNotExist();
		assertThat(RECOMMENDATION.resolve("adapter/out/cache/RedisRecommendationCacheAdapter.java")).doesNotExist();
		assertThat(DOMAIN.resolve("ScoredDiary.java")).doesNotExist();
	}

	@Test
	@DisplayName("Diary와 Feed Application·Domain은 Recommendation 구현 타입을 참조하지 않는다")
	void consumersUseTheirOwnPortsAndCrossContextAdapters() throws IOException {
		for (Path consumer : List.of(
				Path.of("src/main/java/com/pikume/back/diary/application"),
				Path.of("src/main/java/com/pikume/back/diary/domain"),
				Path.of("src/main/java/com/pikume/back/feed/application"),
				Path.of("src/main/java/com/pikume/back/feed/domain"))) {
			assertThat(javaSources(consumer)
					.filter(path -> contains(path, "com.pikume.back.recommendation."))
					.map(Path::toString)
					.toList()).isEmpty();
		}
	}

	@Test
	@DisplayName("Diary 사후 커밋 분석은 독립 트랜잭션이고 선호도 기록은 트랜잭션 안에서 수행한다")
	void preservesApplicationTransactionBoundaries() throws Exception {
		Method analyze = com.pikume.back.recommendation.application.service.DiaryMetadataService.class
				.getMethod("analyzeDiaryContent", Long.class, String.class);
		Transactional analysisTransaction = analyze.getAnnotation(Transactional.class);
		assertThat(analysisTransaction).isNotNull();
		assertThat(analysisTransaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);

		for (String methodName : List.of("recordClick", "recordLike", "recordView", "recordOther")) {
			Method interaction = com.pikume.back.recommendation.application.service.UserPreferenceService.class
					.getMethod(methodName, String.class, String.class);
			assertThat(interaction.getAnnotation(Transactional.class)).isNotNull();
		}
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
