package com.pikume.back.diary.application.port.out;

public interface WebpImageConversionPort {

	byte[] convertToWebp(byte[] imageBytes, float quality);
}
