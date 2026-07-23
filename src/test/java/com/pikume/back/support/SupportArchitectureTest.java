package com.pikume.back.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Support hexagonal architecture")
class SupportArchitectureTest {

	private static final Path SUPPORT = Path.of("src/main/java/com/pikume/back/support");
	private static final Path APPLICATION = SUPPORT.resolve("application");
	private static final Path DOMAIN = SUPPORT.resolve("domain");

	@Test
	@DisplayName("Support Domain은 Application, Adapter, 다른 Context와 외부 기술에 의존하지 않는다")
	void domainDoesNotDependOnOutsideLayersOrContexts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.support.application.",
				"com.pikume.back.support.adapter.",
				"com.pikume.back.user.",
				"org.springframework.",
				"jakarta.mail.");

		assertThat(javaSources(DOMAIN)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Support Application은 Web·Global 파일 DTO·User와 외부 Provider 구현에 의존하지 않는다")
	void applicationUsesTechnicalNeutralContracts() throws IOException {
		List<String> forbiddenDependencies = List.of(
				"com.pikume.back.support.adapter.",
				"com.pikume.back.global.dto.UploadedFileData",
				"com.pikume.back.user.",
				"org.springframework.web.",
				"org.springframework.mail.",
				"jakarta.mail.",
				"software.amazon.");

		assertThat(javaSources(APPLICATION)
				.filter(path -> forbiddenDependencies.stream().anyMatch(fragment -> contains(path, fragment)))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("문의 제출, 제출자 확인, 첨부 저장, 알림 전송과 문의 기록을 목적별 Port로 표현한다")
	void portsExpressSupportPurposes() {
		assertThat(APPLICATION.resolve("port/in/SubmitInquiryUseCase.java")).exists();
		for (String port : List.of(
				"VerifyInquirySubmitterPort.java",
				"StoreInquiryAttachmentPort.java",
				"SendInquiryNotificationPort.java",
				"RecordInquiryPort.java")) {
			assertThat(APPLICATION.resolve("port/out").resolve(port)).exists();
		}
		assertThat(APPLICATION.resolve("port/in/InquiryUseCase.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/LoadUserInfoForSupportPort.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/UploadInquiryImagePort.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/SendFeedbackEmailPort.java")).doesNotExist();
		assertThat(APPLICATION.resolve("port/out/SaveInquiryPort.java")).doesNotExist();
	}

	@Test
	@DisplayName("User만 Cross-context Adapter에 남고 Storage와 Email Adapter는 기술 위치에 둔다")
	void adaptersArePlacedByExternalBoundary() throws IOException {
		Path crossContext = SUPPORT.resolve("adapter/out/crosscontext");

		assertThat(crossContext.resolve("UserAdapterForSupport.java")).exists();
		assertThat(crossContext.resolve("EmailAdapterForSupport.java")).doesNotExist();
		assertThat(crossContext.resolve("InquiryImageUploadAdapter.java")).doesNotExist();
		assertThat(SUPPORT.resolve("adapter/out/email/InquiryEmailAdapter.java")).exists();
		assertThat(SUPPORT.resolve("adapter/out/storage/InquiryAttachmentStorageAdapter.java")).exists();
		assertThat(javaSources(SUPPORT.resolve("adapter/out/email"))
				.filter(path -> contains(path, "com.pikume.back.user.auth."))
				.map(Path::toString)
				.toList()).isEmpty();
	}

	@Test
	@DisplayName("Support Web Adapter는 Application 오류를 RFC 9457 Handler에 위임한다")
	void webAdapterUsesProblemDetailsBoundary() {
		Path web = SUPPORT.resolve("adapter/in/web");
		Path controller = web.resolve("InquiryController.java");

		assertThat(contains(controller, "ProblemDetail.class")).isTrue();
		assertThat(contains(controller, "catch (")).isFalse();
		assertThat(web.resolve("SupportExceptionHandler.java")).exists();
		assertThat(web.resolve("problem/SupportProblemType.java")).exists();
	}

	private java.util.stream.Stream<Path> javaSources(Path root) throws IOException {
		return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
	}

	private boolean contains(Path path, String fragment) {
		try {
			return Files.readString(path).contains(fragment);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
