package com.pikume.back.admin.adapter.out.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("관리자 OTP 암호화 설정")
class AdminOtpCryptoConfigurationTest {

	private static final String VALID_KEY = encode(new byte[32]);
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	@DisplayName("Base64로 인코딩된 32바이트 키면 시작한다")
	void startsWithValidKey() {
		contextRunner
				.withPropertyValues("admin.otp.encryption-key=" + VALID_KEY)
				.run(context -> {
					assertThat(context).hasNotFailed();
					SecretKey key = context.getBean("adminOtpEncryptionKey", SecretKey.class);
					assertThat(key.getAlgorithm()).isEqualTo("AES");
					assertThat(key.getEncoded()).hasSize(32);
				});
	}

	@Test
	@DisplayName("키가 누락되면 시작하지 않는다")
	void rejectsMissingKey() {
		contextRunner.run(context -> assertThat(context)
				.hasFailed()
				.getFailure()
				.hasRootCauseMessage("관리자 OTP 암호화 키는 필수입니다."));
	}

	@Test
	@DisplayName("키가 Base64 형식이 아니면 시작하지 않는다")
	void rejectsMalformedBase64() {
		contextRunner
				.withPropertyValues("admin.otp.encryption-key=not-base64")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasStackTraceContaining("관리자 OTP 암호화 키는 Base64 형식이어야 합니다.");
				});
	}

	@Test
	@DisplayName("Base64 해독 결과가 32바이트가 아니면 시작하지 않는다")
	void rejectsWrongKeyLength() {
		contextRunner
				.withPropertyValues("admin.otp.encryption-key=" + encode(new byte[24]))
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.hasRootCauseMessage("관리자 OTP 암호화 키는 32바이트여야 합니다."));
	}

	private static String encode(byte[] value) {
		return Base64.getEncoder().encodeToString(value);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AdminOtpCryptoProperties.class)
	@Import(AdminOtpCryptoConfiguration.class)
	static class TestConfiguration {
	}
}
