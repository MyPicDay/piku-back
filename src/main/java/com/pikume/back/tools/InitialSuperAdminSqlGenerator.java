package com.pikume.back.tools;

import com.pikume.back.admin.adapter.out.crypto.AdminOtpCryptoProperties;
import com.pikume.back.admin.adapter.out.crypto.AesGcmAdminOtpSecretAdapter;
import com.pikume.back.admin.adapter.out.otp.TotpAdapter;
import com.pikume.back.admin.domain.AdminEmail;
import com.pikume.back.admin.domain.AdminId;
import com.pikume.back.admin.domain.AdminLoginId;
import com.pikume.back.admin.domain.AdminNickname;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.util.Scanner;

public final class InitialSuperAdminSqlGenerator {

	private static final String OTP_ENCRYPTION_KEY_ENV = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
	private static final String OTP_ISSUER = "Pikume Ops";

	private InitialSuperAdminSqlGenerator() {
	}

	public static void main(String[] args) {
		try {
			new InitialSuperAdminSqlGeneratorRunner().run();
		} catch (RuntimeException exception) {
			System.err.println("초기 SUPER_ADMIN SQL 생성 실패: " + exception.getMessage());
			System.exit(1);
		}
	}

	private static final class InitialSuperAdminSqlGeneratorRunner {

		private final Console console = System.console();
		private final Scanner scanner = console == null ? new Scanner(System.in) : null;

		void run() {
			var encryptionKey = new AdminOtpCryptoProperties(OTP_ENCRYPTION_KEY_ENV).secretKey();

			System.out.println("=== 초기 SUPER_ADMIN SQL 생성기 ===");
			if (console == null) {
				System.out.println("주의: 현재 실행 환경에서는 패스워드 입력이 화면에 표시됩니다.");
			}

			String email = AdminEmail.normalize(readLine("이메일: "));
			String loginId = AdminLoginId.normalize(readLine("로그인 ID: "));
			String nickname = AdminNickname.normalize(readLine("닉네임: "));
			String password = readPassword("패스워드: ");
			String passwordConfirmation = readPassword("패스워드 확인: ");
			if (!password.equals(passwordConfirmation)) {
				throw new IllegalArgumentException("패스워드가 일치하지 않습니다.");
			}
			// validatePassword(password, loginId);

			String adminId = AdminId.newId();
			String passwordHash = new BCryptPasswordEncoder().encode(password);
			TotpAdapter totpAdapter = new TotpAdapter();
			String otpSecret = totpAdapter.generateSecret();
			String protectedOtpSecret = new AesGcmAdminOtpSecretAdapter(encryptionKey).protect(otpSecret);
			String provisioningUri = totpAdapter.provisioningUri(OTP_ISSUER, loginId, otpSecret);

			System.out.println();
			System.out.println("OTP 비밀키: " + otpSecret);
			System.out.println("OTP 등록 URI: " + provisioningUri);
			System.out.println("주의: OTP 등록 정보를 인증 앱에 등록한 뒤 안전하게 폐기하세요.");
			System.out.println();
			System.out.println(renderSql(
					adminId, email, loginId, nickname, passwordHash, protectedOtpSecret));
		}

		private String readLine(String prompt) {
			if (console != null) {
				return console.readLine(prompt);
			}
			System.out.print(prompt);
			return scanner.nextLine();
		}

		private String readPassword(String prompt) {
			if (console != null) {
				char[] password = console.readPassword(prompt);
				if (password == null) {
					throw new IllegalArgumentException("패스워드를 입력해야 합니다.");
				}
				return new String(password);
			}
			return readLine(prompt);
		}

		private void validatePassword(String password, String loginId) {
			if (password == null || password.length() < 8 || password.length() > 20
					|| password.chars().anyMatch(Character::isWhitespace)
					|| password.chars().anyMatch(ch -> ch > 127)
					|| password.equals(loginId) || password.contains(loginId)
					|| !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
					|| !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
				throw new IllegalArgumentException("관리자 패스워드 정책을 만족해야 합니다.");
			}
		}

		private String renderSql(String adminId, String email, String loginId, String nickname,
				String passwordHash, String protectedOtpSecret) {
			return """
					START TRANSACTION;

					INSERT INTO admins (
					  id, email, login_id, nickname, status,
					  temporary_password_hash, temporary_password_issued_at, temporary_credential_expires_at,
					  password_hash, password_change_required,
					  otp_registered, otp_registration_required, pending_otp_secret, otp_secret,
					  login_failure_count, locked_until, otp_failure_count, otp_blocked_until,
					  last_login_at, authentication_version, created_at, updated_at, deleted_at
					) VALUES (
					  %s, %s, %s, %s, 'ACTIVE',
					  NULL, NULL, NULL,
					  %s, FALSE,
					  TRUE, FALSE, NULL, %s,
					  0, NULL, 0, NULL,
					  NULL, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
					);

					INSERT INTO admin_roles (admin_id, role)
					VALUES (%s, 'SUPER_ADMIN');

					COMMIT;
					""".formatted(
					sqlLiteral(adminId),
					sqlLiteral(email),
					sqlLiteral(loginId),
					sqlLiteral(nickname),
					sqlLiteral(passwordHash),
					sqlLiteral(protectedOtpSecret),
					sqlLiteral(adminId));
		}

		private String sqlLiteral(String value) {
			return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
		}
	}
}
