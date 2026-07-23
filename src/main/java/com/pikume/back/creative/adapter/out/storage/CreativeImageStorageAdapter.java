package com.pikume.back.creative.adapter.out.storage;

import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.creative.application.port.out.CreativeImageStoragePort;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CreativeImageStorageAdapter implements CreativeImageStoragePort {

	private static final String PRIVATE_CACHE_CONTROL = "private, no-store, max-age=0";

	private final StoreObjectPort storeObjectPort;
	private final ResolveObjectUrlPort resolveObjectUrlPort;
	private final CreativeImageObjectKeyPolicy objectKeyPolicy;

	@Override
	public String resolveGeneratedImageUrl(String objectName, boolean isPublic) {
		return resolveObjectUrlPort.resolveObjectUrl(objectName, isPublic);
	}

	@Override
	public String storeGeneratedImage(String base64Data, String userId, String fileExtension) {
		try {
			if (base64Data == null || base64Data.isBlank()) {
				throw new IllegalArgumentException("생성 이미지 데이터가 비어 있습니다.");
			}
			byte[] imageBytes = Base64.getDecoder().decode(base64Data);
			String objectKey = objectKeyPolicy.createGeneratedImageObjectKey(fileExtension);
			UploadedFileData file = new UploadedFileData(
					"generated" + normalizedExtension(fileExtension),
					objectKeyPolicy.contentType(fileExtension),
					imageBytes);
			return storeObjectPort.storeObject(file, objectKey, PRIVATE_CACHE_CONTROL);
		} catch (RuntimeException exception) {
			throw new CreativeException(CreativeErrorCode.IMAGE_STORAGE_FAILED, exception);
		}
	}

	private String normalizedExtension(String fileExtension) {
		if (fileExtension == null || fileExtension.isBlank()) {
			return "";
		}
		return fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
	}
}
