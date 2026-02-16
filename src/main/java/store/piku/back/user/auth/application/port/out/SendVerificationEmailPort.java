package store.piku.back.user.auth.application.port.out;

import java.util.List;

public interface SendVerificationEmailPort {

	String sendVerificationEmail(String email);

	boolean isEmailAllowed(String email);

	List<String> getAllowedEmailDomains();
}
