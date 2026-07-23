package com.pikume.back.support.application.dto;

public record SubmitInquiryCommand(
		String userId,
		String content,
		InquiryAttachment attachment
) {
}
