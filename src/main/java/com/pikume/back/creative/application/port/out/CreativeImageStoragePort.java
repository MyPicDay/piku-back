package com.pikume.back.creative.application.port.out;

public interface CreativeImageStoragePort {

	String getPhotoUrl(String objectName, boolean isPublic);

	String saveAIPhoto(String base64Data, String userId, String fileExtension);
}
