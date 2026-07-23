package com.pikume.back.global.port.out;

import com.pikume.back.global.dto.UploadedFileData;

public interface StoreObjectPort {

	String storeObject(UploadedFileData file, String objectKey, String cacheControl);
}
