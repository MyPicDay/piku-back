package com.pikume.back.support.adapter.out.crosscontext;

import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdapterForSupport")
class UserAdapterForSupportTest {

	@Mock
	private QueryUserReferenceUseCase queryUserReferenceUseCase;

	@Test
	@DisplayName("User 공개 참조의 존재 여부를 Support 제출자 확인 의미로 번역한다")
	void translatesUserReferenceExistence() {
		given(queryUserReferenceUseCase.queryUserReference("user-1"))
				.willReturn(Optional.of(new UserReferenceView("user-1", "nickname", null)));
		UserAdapterForSupport adapter =
				new UserAdapterForSupport(queryUserReferenceUseCase);

		assertThat(adapter.inquirySubmitterExists("user-1")).isTrue();
	}
}
