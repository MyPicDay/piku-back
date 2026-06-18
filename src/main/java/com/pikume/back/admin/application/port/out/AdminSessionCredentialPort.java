package com.pikume.back.admin.application.port.out;

public interface AdminSessionCredentialPort {

	String generate();

	String hash(String credential);

	boolean matches(String credential, String expectedHash);
}
