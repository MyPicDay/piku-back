package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.domain.AdminAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountPersistenceAdapter")
class AdminAccountPersistenceAdapterTest {

	@Mock AdminAccountJpaRepository adminAccountJpaRepository;
	@Mock AdminAccount adminAccount;

	@Test
	@DisplayName("자격 증명 저장의 유니크 제약 충돌을 로그인 아이디 충돌로 반환한다")
	void returnsFalseForCredentialUniquenessConflict() {
		given(adminAccountJpaRepository.saveAndFlush(adminAccount))
				.willThrow(new DataIntegrityViolationException("duplicate login id"));

		assertThat(adapter().commitIfLoginIdAvailable(adminAccount)).isFalse();
	}

	@Test
	@DisplayName("자격 증명 저장소 장애는 인증 저장소 예외로 변환한다")
	void translatesCredentialStoreFailure() {
		given(adminAccountJpaRepository.saveAndFlush(adminAccount))
				.willThrow(new DataAccessResourceFailureException("db unavailable"));

		assertThatThrownBy(() -> adapter().commitIfLoginIdAvailable(adminAccount))
				.isInstanceOf(AdminAuthenticationStoreException.class);
	}

	private AdminAccountPersistenceAdapter adapter() {
		return new AdminAccountPersistenceAdapter(adminAccountJpaRepository);
	}
}
