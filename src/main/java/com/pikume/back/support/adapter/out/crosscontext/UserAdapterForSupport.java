package com.pikume.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.support.application.port.out.LoadUserInfoForSupportPort;
import com.pikume.back.user.application.port.out.LoadUserPort;

@Component
@RequiredArgsConstructor
public class UserAdapterForSupport implements LoadUserInfoForSupportPort {

	private final LoadUserPort loadUserPort;

	@Override
	public boolean existsById(String userId) {
		return loadUserPort.findById(userId).isPresent();
	}
}
