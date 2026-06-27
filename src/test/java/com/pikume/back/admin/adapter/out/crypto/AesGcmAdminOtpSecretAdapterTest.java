package com.pikume.back.admin.adapter.out.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AesGcmAdminOtpSecretAdapter")
class AesGcmAdminOtpSecretAdapterTest {

	@Test
	@DisplayName("검증된 AES 키로 OTP 비밀키를 암호화하고 복호화한다")
	void encryptsAndDecryptsSecretWithValidatedKey() {
		SecretKey key = new SecretKeySpec(new byte[32], "AES");
		AesGcmAdminOtpSecretAdapter adapter = new AesGcmAdminOtpSecretAdapter(key);

		String first = adapter.protect("JBSWY3DPEHPK3PXP");
		String second = adapter.protect("JBSWY3DPEHPK3PXP");

		assertThat(first).isNotEqualTo(second);
		assertThat(adapter.reveal(first)).isEqualTo("JBSWY3DPEHPK3PXP");
		assertThat(adapter.reveal(second)).isEqualTo("JBSWY3DPEHPK3PXP");
	}
}
