package com.pikume.back.user.application.exception;

import com.pikume.back.user.application.dto.UpdateProfileFailureReason;

public class UpdateProfileFailureException extends RuntimeException {
	private final UpdateProfileFailureReason reason;

	public UpdateProfileFailureException(UpdateProfileFailureReason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public UpdateProfileFailureReason getReason() {
		return reason;
	}
}
