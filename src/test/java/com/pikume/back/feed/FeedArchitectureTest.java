package com.pikume.back.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feed hexagonal architecture")
class FeedArchitectureTest {

	private static final Path FEED = Path.of("src/main/java/com/pikume/back/feed");
	private static final Path APPLICATION = FEED.resolve("application");
	private static final Path DOMAIN = FEED.resolve("domain");

	@Test
	@DisplayName("Feed Application은 다른 Context, Web, Persistence 구현과 트랜잭션 동기화 API에 의존하지 않는다")
	void applicationDependsOnlyOnFeedContractsAndAllowedConfiguration() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.diary.",
				"com.pikume.back.social.",
				"com.pikume.back.user.",
				"com.pikume.back.recommendation.",
				"com.pikume.back.global.",
				"com.pikume.back.feed.adapter.",
				"io.swagger.",
				"org.springframework.web.",
				"org.springframework.security.",
				"org.springframework.data.",
				"org.springframework.transaction.support.",
				"ImagePathToUrlConverter",
				"ResolveImageUrlPort",
				"CustomUserDetails");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Feed Domain은 Application, Adapter, 다른 Context와 Spring 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.feed.application.",
				"com.pikume.back.feed.adapter.",
				"com.pikume.back.diary.",
				"com.pikume.back.social.",
				"com.pikume.back.user.",
				"com.pikume.back.recommendation.",
				"org.springframework.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Feed In Port와 Out Port는 표준 패키지와 접미사를 사용한다")
	void portsUseStandardPackagesAndSuffixes() throws IOException {
		Path inboundPorts = APPLICATION.resolve("port/in");
		Path outboundPorts = APPLICATION.resolve("port/out");

		assertThat(javaSources(inboundPorts)
				.filter(path -> !path.getFileName().toString().endsWith("UseCase.java"))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(javaSources(outboundPorts)
				.filter(path -> !path.getFileName().toString().endsWith("Port.java"))
				.map(Path::toString)
				.toList()).isEmpty();

		for (String port : List.of(
				"QueryFeedDetailUseCase.java",
				"QueryFeedPageUseCase.java",
				"RecordFeedClickUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
	}

	@Test
	@DisplayName("Social 조회 Out Port는 피드 항목, 후보 신호와 친구 관계 목적별로 분리한다")
	void socialQueryPortsAreSplitByFeedPurpose() throws IOException {
		Path outboundPorts = APPLICATION.resolve("port/out");

		for (String port : List.of(
				"LoadFeedItemEngagementPort.java",
				"LoadFeedCandidateSignalsPort.java",
				"LoadFeedFriendshipPort.java")) {
			assertThat(outboundPorts.resolve(port)).exists();
		}
		assertThat(outboundPorts.resolve("LoadFeedEngagementPort.java")).doesNotExist();

		Path services = APPLICATION.resolve("service");
		assertThat(contains(services.resolve("FeedDetailQueryService.java"), "LoadFeedItemEngagementPort")).isTrue();
		assertThat(contains(services.resolve("FeedListItemAssembler.java"), "LoadFeedItemEngagementPort")).isTrue();
		assertThat(contains(services.resolve("FeedListItemAssembler.java"), "LoadFeedFriendshipPort")).isTrue();
		assertThat(contains(services.resolve("RecommendedFeedCandidateService.java"), "LoadFeedCandidateSignalsPort"))
				.isTrue();
	}

	@Test
	@DisplayName("Feed 유스케이스는 상세, 페이지와 클릭 목적별 Service로 분리한다")
	void servicesAreSplitByUseCasePurpose() {
		Path services = APPLICATION.resolve("service");

		for (String service : List.of(
				"FeedDetailQueryService.java",
				"FeedPageQueryService.java",
				"FeedClickService.java",
				"FeedListItemAssembler.java",
				"RecommendedFeedCandidateService.java")) {
			assertThat(services.resolve(service)).exists();
		}
		for (String removedService : List.of(
				"FeedQueryService.java",
				"FeedCompositionService.java",
				"FeedCandidateCollector.java")) {
			assertThat(services.resolve(removedService)).doesNotExist();
		}
	}

	@Test
	@DisplayName("Feed Cross-context Adapter는 표준 폴더에 있고 Provider 저장 계층에 의존하지 않는다")
	void crossContextAdaptersUseStandardFolderAndPublicContracts() throws IOException {
		Path outboundAdapters = FEED.resolve("adapter/out");
		Path crossContextAdapters = outboundAdapters.resolve("crosscontext");

		for (String adapter : List.of(
				"DiaryAdapterForFeed.java",
				"SocialAdapterForFeed.java",
				"UserAdapterForFeed.java",
				"RecommendationAdapterForFeed.java")) {
			assertThat(crossContextAdapters.resolve(adapter)).exists();
		}
		for (String legacyFolder : List.of("diary", "social", "user", "recommendation")) {
			Path folder = outboundAdapters.resolve(legacyFolder);
			assertThat(Files.exists(folder) && javaSources(folder).findAny().isPresent()).isFalse();
		}
		assertThat(javaSources(crossContextAdapters)
				.filter(path -> contains(path, ".adapter.out.persistence")
						|| contains(path, "com.pikume.back.diary.application.port.out")
						|| contains(path, "com.pikume.back.social.application.port.out")
						|| contains(path, "com.pikume.back.user.application.port.out")
						|| contains(path, "com.pikume.back.recommendation.application.port.out"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Feed Persistence Adapter는 FeedClick 조회와 기록만 담당한다")
	void persistenceAdaptersOnlyHandleFeedClick() throws IOException {
		Path persistence = FEED.resolve("adapter/out/persistence");

		assertThat(javaSources(persistence)
				.map(path -> path.getFileName().toString())
				.sorted()
				.toList())
				.containsExactly("FeedClickJpaRepository.java", "FeedClickPersistenceAdapter.java");
	}

	@Test
	@DisplayName("Feed Web Adapter는 Out Port를 직접 호출하지 않고 Problem Details를 명세한다")
	void webAdapterUsesInboundPortsAndProblemDetails() throws IOException {
		Path web = FEED.resolve("adapter/in/web");
		Path controller = web.resolve("FeedController.java");

		assertThat(javaSources(web)
				.filter(path -> contains(path, "feed.application.port.out"))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(contains(controller, "QueryFeedDetailUseCase")).isTrue();
		assertThat(contains(controller, "QueryFeedPageUseCase")).isTrue();
		assertThat(contains(controller, "RecordFeedClickUseCase")).isTrue();
		assertThat(contains(controller, "MediaType.APPLICATION_PROBLEM_JSON_VALUE")).isTrue();
		assertThat(contains(controller, "ProblemDetail.class")).isTrue();
	}

	@Test
	@DisplayName("Feed에는 구조 리팩터링 범위를 벗어난 트랜잭션 완료 Port가 없다")
	void transactionCompletionPortIsDeferredToFollowUpWork() throws IOException {
		assertThat(javaSources(FEED)
				.filter(path -> contains(path, "TransactionCompletionPort")
						|| contains(path, "TransactionSynchronization"))
				.map(Path::toString)
				.toList()).isEmpty();
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
