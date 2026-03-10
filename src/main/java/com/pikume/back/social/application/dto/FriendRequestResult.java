package com.pikume.back.social.application.dto;

public record FriendRequestResult(
		boolean accepted,
		String message) {
}
