package com.pikume.back.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification hexagonal architecture")
class NotificationArchitectureTest {

	private static final Path NOTIFICATION =
			Path.of("src/main/java/com/pikume/back/notification");
	private static final Path APPLICATION = NOTIFICATION.resolve("application");
	private static final Path DOMAIN = NOTIFICATION.resolve("domain");

	@Test
	@DisplayName("Notification Application은 다른 Context와 외부 기술 구현에 의존하지 않는다")
	void applicationDependsOnlyOnNotificationContractsAndAllowedConfiguration() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.diary.",
				"com.pikume.back.social.",
				"com.pikume.back.user.",
				"com.pikume.back.notification.adapter.",
				"io.swagger.",
				"org.springframework.web.",
				"org.springframework.security.",
				"org.springframework.data.",
				"org.springframework.transaction.support.",
				"com.google.firebase.",
				"FirebaseMessaging",
				"JpaRepository");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Notification Domain은 Application, Adapter, 다른 Context와 Spring 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.notification.application.",
				"com.pikume.back.notification.adapter.",
				"com.pikume.back.diary.",
				"com.pikume.back.social.",
				"com.pikume.back.user.",
				"org.springframework.",
				"com.google.firebase.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Notification In Port와 Out Port는 목적별 표준 패키지와 접미사를 사용한다")
	void portsUseStandardPackagesAndPurposeSpecificNames() throws IOException {
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
				"RecordNotificationUseCase.java",
				"QueryNotificationPageUseCase.java",
				"MarkNotificationReadUseCase.java",
				"MarkAllNotificationsReadUseCase.java",
				"DeleteNotificationUseCase.java",
				"DeleteNotificationsByDiaryUseCase.java",
				"RegisterPushTokenUseCase.java",
				"RevokePushTokenUseCase.java",
				"SubscribeNotificationStreamUseCase.java",
				"DeliverNotificationUseCase.java")) {
			assertThat(inboundPorts.resolve(port)).exists();
		}
		for (String legacyPort : List.of(
				"NotificationUseCase.java",
				"FcmTokenUseCase.java",
				"SseUseCase.java",
				"SaveNotificationPort.java",
				"DeleteNotificationPort.java",
				"PushNotificationPort.java",
				"NotificationStreamPort.java",
				"LoadNotificationListViewPort.java",
				"LoadNotificationPort.java")) {
			assertThat(inboundPorts.resolve(legacyPort)).doesNotExist();
			assertThat(outboundPorts.resolve(legacyPort)).doesNotExist();
		}
		assertThat(outboundPorts.resolve("LoadActiveNotificationPort.java")).exists();
		assertThat(outboundPorts.resolve("MarkNotificationReadPort.java")).exists();
	}

	@Test
	@DisplayName("Notification Application Service는 유스케이스 목적별 클래스로 분리한다")
	void servicesAreSplitByUseCasePurpose() {
		Path services = APPLICATION.resolve("service");

		for (String service : List.of(
				"NotificationRecordingService.java",
				"NotificationPageQueryService.java",
				"NotificationReadService.java",
				"NotificationDeletionService.java",
				"NotificationDeliveryService.java",
				"NotificationStreamSubscriptionService.java",
				"PushTokenService.java",
				"NotificationListAssembler.java")) {
			assertThat(services.resolve(service)).exists();
		}
		assertThat(services.resolve("NotificationService.java")).doesNotExist();
		assertThat(services.resolve("FcmTokenService.java")).doesNotExist();
		assertThat(services.resolve("SseSubscriptionService.java")).doesNotExist();
	}

	@Test
	@DisplayName("Notification Application Service는 책임에 해당하는 공개 In Port를 구현한다")
	void servicesImplementPurposeSpecificInboundPorts() {
		Path services = APPLICATION.resolve("service");

		assertThat(contains(services.resolve("NotificationRecordingService.java"),
				"implements RecordNotificationUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationPageQueryService.java"),
				"implements QueryNotificationPageUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationReadService.java"),
				"implements MarkNotificationReadUseCase, MarkAllNotificationsReadUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationReadService.java"),
				"MarkNotificationReadPort")).isTrue();
		assertThat(contains(services.resolve("NotificationReadService.java"),
				"LoadNotificationPort")).isFalse();
		assertThat(contains(services.resolve("NotificationDeletionService.java"),
				"implements DeleteNotificationUseCase, DeleteNotificationsByDiaryUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationDeletionService.java"),
				"LoadActiveNotificationPort")).isTrue();
		assertThat(contains(services.resolve("NotificationDeletionService.java"),
				"LoadNotificationPort")).isFalse();
		assertThat(contains(services.resolve("PushTokenService.java"),
				"implements RegisterPushTokenUseCase, RevokePushTokenUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationStreamSubscriptionService.java"),
				"implements SubscribeNotificationStreamUseCase")).isTrue();
		assertThat(contains(services.resolve("NotificationDeliveryService.java"),
				"implements DeliverNotificationUseCase")).isTrue();
	}

	@Test
	@DisplayName("Notification Cross-context Adapter는 Provider 공개 계약을 Notification 모델로 번역한다")
	void crossContextAdaptersUsePublicProviderContracts() throws IOException {
		Path crossContextAdapters = NOTIFICATION.resolve("adapter/out/crosscontext");

		assertThat(crossContextAdapters.resolve("DiaryAdapterForNotification.java")).exists();
		assertThat(crossContextAdapters.resolve("UserAdapterForNotification.java")).exists();
		assertThat(javaSources(crossContextAdapters)
				.filter(path -> contains(path, ".adapter.out.persistence")
						|| contains(path, "com.pikume.back.diary.domain")
						|| contains(path, "com.pikume.back.diary.application.port.out")
						|| contains(path, "com.pikume.back.user.domain")
						|| contains(path, "com.pikume.back.user.application.port.out"))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(contains(
				crossContextAdapters.resolve("DiaryAdapterForNotification.java"),
				"NotificationDiaryContextView")).isTrue();
		assertThat(contains(
				crossContextAdapters.resolve("DiaryAdapterForNotification.java"),
				"diary.application.port.in")).isTrue();
		assertThat(contains(
				crossContextAdapters.resolve("UserAdapterForNotification.java"),
				"NotificationSenderView")).isTrue();
		assertThat(contains(
				crossContextAdapters.resolve("UserAdapterForNotification.java"),
				"user.application.port.in")).isTrue();
	}

	@Test
	@DisplayName("알림 전달 시점은 Notification 소유 Port 뒤의 트랜잭션 Adapter가 결정한다")
	void notificationOwnsDeliverySchedulingPort() throws IOException {
		Path schedulePort = APPLICATION.resolve("port/out/ScheduleNotificationDeliveryPort.java");
		Path schedulerAdapter =
				NOTIFICATION.resolve("adapter/out/transaction/SpringNotificationDeliverySchedulerAdapter.java");

		assertThat(schedulePort).exists();
		assertThat(contains(schedulePort, "NotificationDeliveryRequest")).isTrue();
		assertThat(contains(schedulePort, "Runnable")).isFalse();
		assertThat(contains(schedulePort, "org.springframework.")).isFalse();
		assertThat(contains(schedulerAdapter, "ScheduleNotificationDeliveryPort")).isTrue();
		assertThat(contains(schedulerAdapter, "TransactionSynchronizationManager")).isTrue();
		assertThat(javaSources(APPLICATION)
				.filter(path -> contains(path, "TransactionSynchronization"))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Persistence와 Push Adapter는 각 기술 책임을 분리한다")
	void persistenceAndPushAdaptersKeepSeparateResponsibilities() throws IOException {
		Path persistence = NOTIFICATION.resolve("adapter/out/persistence");
		Path push = NOTIFICATION.resolve("adapter/out/push");

		assertThat(persistence.resolve("NotificationListViewPersistenceAdapter.java")).doesNotExist();
		assertThat(javaSources(persistence)
				.filter(path -> contains(path, "com.pikume.back.diary.")
						|| contains(path, "com.pikume.back.user."))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(contains(push.resolve("FcmPushAdapter.java"), "FcmTokenJpaRepository")).isFalse();
		assertThat(contains(push.resolve("LocalPushAdapter.java"), "RegisterPushTokenPort")).isFalse();
		Path productionTokenAdapter = persistence.resolve("FcmTokenPersistenceAdapter.java");
		Path localTokenAdapter = NOTIFICATION.resolve("adapter/out/token/LocalPushTokenAdapter.java");
		for (Path adapter : List.of(productionTokenAdapter, localTokenAdapter)) {
			assertThat(contains(adapter, "LoadPushDeliveryTokensPort")).isTrue();
			assertThat(contains(adapter, "RegisterPushTokenPort")).isTrue();
			assertThat(contains(adapter, "RevokePushTokenPort")).isTrue();
		}
		assertThat(contains(productionTokenAdapter, "@Profile(\"prod\")")).isTrue();
		assertThat(contains(localTokenAdapter, "@Profile(\"!prod\")")).isTrue();
	}

	@Test
	@DisplayName("Notification Web Adapter는 In Port만 호출하고 RFC 9457 오류를 명세한다")
	void webAdaptersUseInboundPortsAndProblemDetails() throws IOException {
		Path web = NOTIFICATION.resolve("adapter/in/web");
		Path controller = web.resolve("NotificationController.java");

		assertThat(javaSources(web)
				.filter(path -> contains(path, "notification.application.port.out")
						|| contains(path, "notification.domain."))
				.map(Path::toString)
				.toList()).isEmpty();
		assertThat(contains(controller, "QueryNotificationPageUseCase")).isTrue();
		assertThat(contains(controller, "MarkNotificationReadUseCase")).isTrue();
		assertThat(contains(controller, "DeleteNotificationUseCase")).isTrue();
		assertThat(contains(controller, "MediaType.APPLICATION_PROBLEM_JSON_VALUE")).isTrue();
		assertThat(contains(controller, "ProblemDetail.class")).isTrue();
	}

	@Test
	@DisplayName("Social 알림 사건은 Social Application의 공개 계약으로 둔다")
	void socialNotificationEventsArePublishedApplicationContracts() {
		Path publishedEvent =
				Path.of("src/main/java/com/pikume/back/social/application/event/SocialNotificationEvent.java");
		Path legacyEvent =
				Path.of("src/main/java/com/pikume/back/social/domain/event/SocialEvent.java");

		assertThat(publishedEvent).exists();
		assertThat(legacyEvent).doesNotExist();
		assertThat(contains(
				NOTIFICATION.resolve("adapter/in/event/SocialEventListener.java"),
				"social.application.event.SocialNotificationEvent")).isTrue();
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
