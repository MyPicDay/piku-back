package com.pikume.back.user.auth.application.port.out;

public interface PasswordProtectionPort {

	String protect(String rawPassword);

	boolean matches(String rawPassword, String protectedPassword);
}
