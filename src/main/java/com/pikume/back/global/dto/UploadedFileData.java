package com.pikume.back.global.dto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public record UploadedFileData(String originalFilename, String contentType, byte[] bytes) {

	public UploadedFileData {
		bytes = bytes == null ? new byte[0] : bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}

	public boolean isEmpty() {
		return bytes.length == 0;
	}

	public long size() {
		return bytes.length;
	}

	public InputStream inputStream() {
		return new ByteArrayInputStream(bytes);
	}
}
