package com.pikume.back.admin.application.port.out;

public interface ProtectAdminOtpSecretPort {

	String protect(String plainSecret);

	String reveal(String protectedSecret);
}
