package com.pikume.back.global.port.out;

import com.pikume.back.global.dto.UploadedFileData;

public interface StoreObjectPort {

	default String storeObject(UploadedFileData file, String objectKey) {
		return storeObject(file, objectKey, null);
	}

	String storeObject(UploadedFileData file, String objectKey, String cacheControl);
}
