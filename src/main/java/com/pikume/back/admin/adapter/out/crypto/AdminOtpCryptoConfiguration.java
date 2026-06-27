package com.pikume.back.admin.adapter.out.crypto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

/**
 * 관리자 OTP 비밀키 암호화에 사용할 AES 키를 Spring 빈으로 구성한다.
 *
 * <p>{@link AdminOtpCryptoProperties}에서 설정값의 형식과 길이를 검증한 뒤 생성한 키만
 * 암호화 어댑터에 주입될 수 있도록 애플리케이션 설정과 암호화 구현 사이를 조립한다.</p>
 */
@Configuration(proxyBeanMethods = false)
public class AdminOtpCryptoConfiguration {

	public static final String ADMIN_OTP_ENCRYPTION_KEY_BEAN = "adminOtpEncryptionKey";

	/**
	 * 검증이 끝난 관리자 OTP 암호화 키를 이름 있는 빈으로 등록한다.
	 *
	 * @param properties {@code admin.otp} 설정을 바인딩하고 검증한 속성
	 * @return AES-GCM 암복호화에 사용할 AES 키
	 */
	@Bean(ADMIN_OTP_ENCRYPTION_KEY_BEAN)
	SecretKey adminOtpEncryptionKey(AdminOtpCryptoProperties properties) {
		return properties.secretKey();
	}
}
