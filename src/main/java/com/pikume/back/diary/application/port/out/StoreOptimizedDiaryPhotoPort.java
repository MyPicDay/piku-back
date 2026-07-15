package com.pikume.back.diary.application.port.out;

public interface StoreOptimizedDiaryPhotoPort {

	void store(String objectKey, String contentType, byte[] bytes);
}
