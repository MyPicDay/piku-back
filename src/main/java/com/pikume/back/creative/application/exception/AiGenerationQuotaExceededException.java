package com.pikume.back.creative.application.exception;

public class AiGenerationQuotaExceededException extends CreativeException {

	private final int dailyLimit;

	public AiGenerationQuotaExceededException(int dailyLimit) {
		super(
				CreativeErrorCode.QUOTA_EXCEEDED,
				"일일 생성 횟수(" + dailyLimit + "회)를 모두 사용하셨습니다.");
		this.dailyLimit = dailyLimit;
	}

	public int dailyLimit() {
		return dailyLimit;
	}
}
