package com.pikume.back.user.application.port.out;

public interface CheckUserUniquenessPort {

	boolean isNicknameInUse(String nickname);

	boolean isEmailRegistered(String email);
}
