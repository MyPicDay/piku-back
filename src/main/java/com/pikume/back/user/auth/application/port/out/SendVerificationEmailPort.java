package com.pikume.back.user.auth.application.port.out;

public interface SendVerificationEmailPort {

	String sendVerificationEmail(String email);

}
