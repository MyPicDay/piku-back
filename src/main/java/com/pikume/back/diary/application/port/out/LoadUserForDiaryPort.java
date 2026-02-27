package com.pikume.back.diary.application.port.out;

public interface LoadUserForDiaryPort {
	String getUserNickname(String userId);

	String getUserAvatar(String userId);

	boolean existsById(String userId);
}
