package com.pikume.back.user.application.port.out;

public interface CheckUserUniquenessPort {

	boolean existsByNickname(String nickname);

	boolean existsByEmail(String email);
}
