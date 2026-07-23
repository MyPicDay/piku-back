package com.pikume.back.diary.adapter.out.storage;

import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.LoadObjectPort;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 다른 패키지가 중립 저장 Adapter로 이동하기 전까지 기존 저장 Port를 연결하는 호환 Adapter다.
 */
@Component
@RequiredArgsConstructor
public class SharedImageStorageCompatibilityAdapter
		implements ResolveImageUrlPort, StoreObjectPort, LoadObjectPort {

	private final MinioPhotoStorageAdapter storageAdapter;

	@Override
	public String getPhotoUrl(String objectName, boolean isPublic) {
		return storageAdapter.getPhotoUrl(objectName, isPublic);
	}

	@Override
	public String storeObject(UploadedFileData image, String objectKey, String cacheControl) {
		return storageAdapter.storeObject(image.contentType(), image.bytes(), objectKey, cacheControl);
	}

	@Override
	public byte[] loadObject(String objectKey) {
		return storageAdapter.loadObject(objectKey);
	}
}
