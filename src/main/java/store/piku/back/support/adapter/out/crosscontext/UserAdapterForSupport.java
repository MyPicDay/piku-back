package store.piku.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.support.application.port.out.LoadUserInfoForSupportPort;
import store.piku.back.user.application.port.out.LoadUserPort;

@Component
@RequiredArgsConstructor
public class UserAdapterForSupport implements LoadUserInfoForSupportPort {

	private final LoadUserPort loadUserPort;

	@Override
	public boolean existsById(String userId) {
		return loadUserPort.findById(userId).isPresent();
	}
}
