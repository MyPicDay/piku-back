package com.pikume.back.admin.application.port.out;

public interface AdminPasswordPort {

	String encode(String rawPassword);

	boolean matches(String rawPassword, String encodedPassword);
}
