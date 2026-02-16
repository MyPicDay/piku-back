package store.piku.back.user.auth.application.port.out;

import store.piku.back.user.auth.domain.VerifiedEmail;

public interface SaveVerifiedEmailPort {

	VerifiedEmail save(VerifiedEmail verifiedEmail);
}
