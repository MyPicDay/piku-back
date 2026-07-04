package com.pikume.back.creative.application.port.out;

public interface CreativeImageStoragePort {

	String resolveGeneratedImageUrl(String objectName, boolean isPublic);

	String storeGeneratedImage(String base64Data, String userId, String fileExtension);
}
