package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdminAccount")
class AdminAccountTest {

	private final LocalDateTime now = LocalDateTime.of(2026, 6, 17, 13, 0);

	@Nested
	@DisplayName("초대 생성")
	class Invitation {

		@Test
		@DisplayName("관리자 계정을 ACTIVE 상태와 최초 설정 필요 상태로 생성한다")
		void createsInvitedAdmin() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			assertThat(admin.getId()).isNotBlank();
			assertThat(admin.getEmail()).isEqualTo("operator@pikume.com");
			assertThat(admin.getNickname()).isEqualTo("운영자1");
			assertThat(admin.getRole()).isEqualTo(AdminRole.OPERATOR);
			assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.ACTIVE);
			assertThat(admin.isPasswordChangeRequired()).isTrue();
			assertThat(admin.isOtpRegistrationRequired()).isTrue();
			assertThat(admin.isOtpRegistered()).isFalse();
			assertThat(admin.canUseTemporaryCredentialAt(now.plusHours(1))).isTrue();
		}

		@Test
		@DisplayName("임시 자격 증명 만료 시각이 지나면 임시 로그인을 사용할 수 없다")
		void expiredTemporaryCredentialCannotBeUsed() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			assertThat(admin.canUseTemporaryCredentialAt(now.plusDays(1).plusNanos(1))).isFalse();
		}
	}

	@Nested
	@DisplayName("정식 로그인 아이디")
	class LoginId {

		@Test
		@DisplayName("정식 로그인 아이디는 한 번만 설정할 수 있다")
		void loginIdCanBeSetOnlyOnce() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			admin.setLoginId("ops-june");

			assertThat(admin.getLoginId()).isEqualTo("ops-june");
			assertThatThrownBy(() -> admin.setLoginId("ops-next"))
					.isInstanceOf(AdminDomainException.class);
		}

		@Test
		@DisplayName("정식 로그인 아이디는 정책에 맞아야 한다")
		void loginIdMustMatchPolicy() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			assertThatThrownBy(() -> admin.setLoginId("운영자"))
					.isInstanceOf(AdminDomainException.class);
			assertThatThrownBy(() -> admin.setLoginId("ABC"))
					.isInstanceOf(AdminDomainException.class);
			assertThatThrownBy(() -> admin.setLoginId("admin login"))
					.isInstanceOf(AdminDomainException.class);
		}
	}

	@Nested
	@DisplayName("패스워드와 OTP 설정")
	class PasswordAndOtp {

		@Test
		@DisplayName("정식 패스워드 설정 시 임시 패스워드를 무효화한다")
		void passwordSetupInvalidatesTemporaryPassword() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			admin.completePasswordSetup("bcrypt-hash");

			assertThat(admin.getPasswordHash()).isEqualTo("bcrypt-hash");
			assertThat(admin.isPasswordChangeRequired()).isFalse();
			assertThat(admin.getTemporaryPasswordHash()).isNull();
			assertThat(admin.getTemporaryCredentialExpiresAt()).isNull();
			assertThat(admin.canUseTemporaryCredentialAt(now)).isFalse();
		}

		@Test
		@DisplayName("OTP 등록 완료 시 OTP 재등록 필요 상태와 실패 횟수를 초기화한다")
		void otpRegistrationResetsOtpState() {
			AdminAccount admin = invited(AdminRole.OPERATOR);
			admin.recordOtpFailure(now);

			admin.completeOtpRegistration();

			assertThat(admin.isOtpRegistered()).isTrue();
			assertThat(admin.isOtpRegistrationRequired()).isFalse();
			assertThat(admin.getOtpFailureCount()).isZero();
			assertThat(admin.getOtpBlockedUntil()).isNull();
		}
	}

	@Nested
	@DisplayName("잠금 정책")
	class LockPolicy {

		@Test
		@DisplayName("패스워드 로그인 5회 실패 시 30분 동안 계정을 잠근다")
		void locksAfterFivePasswordFailures() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			for (int i = 0; i < 5; i++) {
				admin.recordPasswordFailure(now);
			}

			assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.LOCKED);
			assertThat(admin.getLoginFailureCount()).isEqualTo(5);
			assertThat(admin.getLockedUntil()).isEqualTo(now.plusMinutes(30));
			assertThat(admin.isLockedAt(now.plusMinutes(29))).isTrue();
		}

		@Test
		@DisplayName("자동 잠금 시간이 지나면 잠금을 해제할 수 있다")
		void releasesExpiredLock() {
			AdminAccount admin = invited(AdminRole.OPERATOR);
			for (int i = 0; i < 5; i++) {
				admin.recordPasswordFailure(now);
			}

			admin.releaseExpiredLock(now.plusMinutes(30));

			assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.ACTIVE);
			assertThat(admin.getLoginFailureCount()).isZero();
			assertThat(admin.getLockedUntil()).isNull();
		}

		@Test
		@DisplayName("OTP 5회 실패 시 10분 동안 OTP 인증을 차단한다")
		void blocksOtpAfterFiveFailures() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			for (int i = 0; i < 5; i++) {
				admin.recordOtpFailure(now);
			}

			assertThat(admin.getOtpFailureCount()).isEqualTo(5);
			assertThat(admin.getOtpBlockedUntil()).isEqualTo(now.plusMinutes(10));
			assertThat(admin.isOtpBlockedAt(now.plusMinutes(9))).isTrue();
		}
	}

	@Nested
	@DisplayName("계정 운영")
	class AccountOperation {

		@Test
		@DisplayName("비활성화에는 사유가 필요하다")
		void deactivationRequiresReason() {
			AdminAccount admin = invited(AdminRole.OPERATOR);

			assertThatThrownBy(() -> admin.deactivate(" "))
					.isInstanceOf(AdminDomainException.class);

			admin.deactivate("퇴사");

			assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.INACTIVE);
		}

		@Test
		@DisplayName("재활성화 시 임시 패스워드를 새로 발급하고 패스워드 변경을 요구한다")
		void reactivationReissuesTemporaryPassword() {
			AdminAccount admin = invited(AdminRole.OPERATOR);
			admin.completePasswordSetup("old-hash");
			admin.deactivate("퇴사");

			admin.reactivate("new-temp-hash", now.plusHours(1), now.plusHours(25));

			assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.ACTIVE);
			assertThat(admin.getTemporaryPasswordHash()).isEqualTo("new-temp-hash");
			assertThat(admin.getTemporaryCredentialExpiresAt()).isEqualTo(now.plusHours(25));
			assertThat(admin.isPasswordChangeRequired()).isTrue();
		}

		@Test
		@DisplayName("OTP 초기화 시 OTP 재등록이 필요하다")
		void resetOtpRequiresRegistrationAgain() {
			AdminAccount admin = invited(AdminRole.OPERATOR);
			admin.completeOtpRegistration();

			admin.resetOtp();

			assertThat(admin.isOtpRegistered()).isFalse();
			assertThat(admin.isOtpRegistrationRequired()).isTrue();
		}
	}

	private AdminAccount invited(AdminRole role) {
		return AdminAccount.invite(
				"Operator@Pikume.com",
				"운영자1",
				role,
				"temp-hash",
				now,
				now.plusDays(1));
	}
}
