package store.piku.back.user.auth.application.port.out;

import store.piku.back.user.auth.domain.Verification;

public interface SaveVerificationPort {

	Verification save(Verification verification);

	void delete(Verification verification);
}
