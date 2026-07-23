package com.pikume.back.support.application.dto;

public record InquiryAttachment(String originalFilename, String contentType, byte[] bytes) {

	public InquiryAttachment {
		bytes = bytes == null ? new byte[0] : bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}

	public boolean isEmpty() {
		return bytes.length == 0;
	}
}
