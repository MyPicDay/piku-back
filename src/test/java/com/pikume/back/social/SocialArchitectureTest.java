package com.pikume.back.social;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Social hexagonal architecture")
class SocialArchitectureTest {

	private static final Path SOCIAL = Path.of("src/main/java/com/pikume/back/social");
	private static final Path APPLICATION = SOCIAL.resolve("application");
	private static final Path DOMAIN = SOCIAL.resolve("domain");

	@Test
	@DisplayName("Social Domain은 Application, Adapter, 다른 Context와 Spring 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.social.application.",
				"com.pikume.back.social.adapter.",
				"com.pikume.back.global.entity.",
				"com.pikume.back.diary.",
				"com.pikume.back.feed.",
				"com.pikume.back.notification.",
				"com.pikume.back.user.",
				"org.springframework.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Social Application은 Adapter, 다른 Context, Web, Spring Data와 이미지 URL 기술에 의존하지 않는다")
	void applicationDependsOnlyOnSocialContractsAndAllowedConfiguration() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.social.adapter.",
				"com.pikume.back.diary.",
				"com.pikume.back.feed.",
				"com.pikume.back.notification.",
				"com.pikume.back.user.",
				"org.springframework.dao.",
				"org.springframework.data.",
				"org.springframework.web.",
				"org.springframework.security.",
				"ImagePathToUrlConverter");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(javaSources(APPLICATION)
				.filter(path -> contains(path, "Object[]"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Social Port와 Service는 유스케이스 목적별 계약을 사용한다")
	void portsAndServicesAreSplitByPurpose() throws IOException {
		Path inboundPorts = APPLICATION.resolve("port/in");
		Path outboundPorts = APPLICATION.resolve("port/out");
		Path services = APPLICATION.resolve("service");

		assertThat(javaSources(inboundPorts)
				.filter(path -> !path.getFileName().toString().endsWith("UseCase.java"))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(javaSources(outboundPorts)
				.filter(path -> !path.getFileName().toString().endsWith("Port.java"))
				.map(Path::toString)
				.toList()).isEmpty();
		List<String> persistenceIdioms = List.of(" findBy", " save(", "ForUpdate", "IfAbsent", "Object[]");
		assertThat(javaSources(outboundPorts)
				.filter(path -> persistenceIdioms.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();

		for (String port : List.of(
				"SendFriendRequestUseCase.java",
				"RejectFriendRequestUseCase.java",
				"CancelFriendRequestUseCase.java",
				"RemoveFriendshipUseCase.java",
				"QueryFriendPageUseCase.java",
				"QueryFriendshipUseCase.java",
				"CreateCommentUseCase.java",
				"UpdateCommentUseCase.java",
				"DeleteCommentUseCase.java",
				"QueryCommentPageUseCase.java",
				"QueryCommentEngagementUseCase.java",
				"AddDiaryLikeUseCase.java",
				"RemoveDiaryLikeUseCase.java",
				"QueryDiaryLikeEngagementUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
		for (String obsoletePort : List.of("FriendUseCase.java", "CommentUseCase.java", "LikeUseCase.java")) {
			assertThat(inboundPorts.resolve(obsoletePort)).doesNotExist();
		}
		for (String port : List.of(
				"LoadFriendshipsPort.java",
				"RecordFriendshipPort.java",
				"LoadPendingFriendRequestsPort.java",
				"RecordFriendRequestPort.java",
				"LoadCommentThreadsPort.java",
				"LoadCommentEngagementPort.java",
				"RecordCommentPort.java",
				"LoadDiaryLikesPort.java",
				"RecordDiaryLikePort.java",
				"VerifySocialParticipantPort.java",
				"LoadSocialParticipantProfilesPort.java",
				"ResolveInteractionDiaryPort.java",
				"PublishSocialNotificationEventPort.java")) {
			assertThat(outboundPorts.resolve(port)).exists();
		}
		for (String obsoletePort : List.of(
				"LoadFriendPort.java",
				"SaveFriendPort.java",
				"LoadFriendRequestPort.java",
				"SaveFriendRequestPort.java",
				"LoadCommentPort.java",
				"SaveCommentPort.java",
				"LoadCommentListViewPort.java",
				"LoadFriendListViewPort.java",
				"LoadLikePort.java",
				"SaveLikePort.java",
				"LoadUserInfoPort.java",
				"LoadDiaryInfoPort.java")) {
			assertThat(outboundPorts.resolve(obsoletePort)).doesNotExist();
		}
		for (String service : List.of(
				"FriendCommandService.java",
				"FriendQueryService.java",
				"CommentCommandService.java",
				"CommentQueryService.java",
				"LikeCommandService.java",
				"LikeQueryService.java")) {
			assertThat(services.resolve(service)).exists();
		}
		for (String obsoleteService : List.of("FriendService.java", "CommentService.java", "LikeService.java")) {
			assertThat(services.resolve(obsoleteService)).doesNotExist();
		}
	}

	@Test
	@DisplayName("Social Persistence Adapter는 Social 데이터만 다룬다")
	void persistenceAdaptersDoNotComposeOtherContexts() throws IOException {
		Path persistence = SOCIAL.resolve("adapter/out/persistence");

		assertThat(javaSources(persistence)
				.filter(path -> contains(path, "com.pikume.back.user.")
						|| contains(path, "com.pikume.back.diary."))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(persistence.resolve("FriendListViewPersistenceAdapter.java")).doesNotExist();
		assertThat(persistence.resolve("CommentListViewPersistenceAdapter.java")).doesNotExist();
	}

	@Test
	@DisplayName("Social Cross-context Adapter는 공급자의 공개 Application 계약만 사용한다")
	void crossContextAdaptersUseOnlyPublicProviderContracts() throws IOException {
		Path crossContext = SOCIAL.resolve("adapter/out/crosscontext");

		assertThat(javaSources(crossContext)
				.filter(path -> contains(path, ".adapter.out.persistence")
						|| contains(path, "com.pikume.back.diary.domain.")
						|| contains(path, "com.pikume.back.diary.application.port.out")
						|| contains(path, "com.pikume.back.user.domain.")
						|| contains(path, "com.pikume.back.user.application.port.out"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("다른 Context는 Social 공개 Application 계약만 사용한다")
	void otherContextsDoNotDependOnSocialInternals() throws IOException {
		Path production = Path.of("src/main/java/com/pikume/back");

		assertThat(javaSources(production)
				.filter(path -> !path.startsWith(SOCIAL))
				.filter(path -> contains(path, "com.pikume.back.social.domain.")
						|| contains(path, "com.pikume.back.social.application.port.out")
						|| contains(path, "com.pikume.back.social.adapter.out.persistence"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Social 알림 사건 발행 Port는 공개 사건 타입만 허용한다")
	void notificationEventPublisherIsTypeSafe() {
		Path outboundPorts = APPLICATION.resolve("port/out");
		Path crossContext = SOCIAL.resolve("adapter/out/crosscontext");
		Path eventPort = outboundPorts.resolve("PublishSocialNotificationEventPort.java");

		assertThat(eventPort).exists();
		assertThat(contains(eventPort, "SocialNotificationEvent")).isTrue();
		assertThat(contains(eventPort, "Object event")).isFalse();
		assertThat(outboundPorts.resolve("PublishEventPort.java")).doesNotExist();
		assertThat(crossContext.resolve("SpringSocialNotificationEventPublisher.java")).exists();
		assertThat(crossContext.resolve("SpringEventPublishAdapter.java")).doesNotExist();
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
