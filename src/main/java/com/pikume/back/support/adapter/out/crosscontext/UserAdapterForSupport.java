package com.pikume.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.support.application.port.out.LoadUserInfoForSupportPort;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;

@Component
@RequiredArgsConstructor
public class UserAdapterForSupport implements LoadUserInfoForSupportPort {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase;

	@Override
	public boolean existsById(String userId) {
		return queryUserReferenceUseCase.queryUserReference(userId).isPresent();
	}
}
