package com.pikume.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.StoreObjectPort;
import com.pikume.back.support.application.port.out.UploadInquiryImagePort;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InquiryImageUploadAdapter implements UploadInquiryImagePort {

	private final StoreObjectPort storeObjectPort;

	@Override
	public String upload(UploadedFileData image, String userId) {
		String originalFilename = image.originalFilename();
		String filename = generateFileName(originalFilename);
		String uuid = userId.substring(0, 8);
		String objectKey = "inquiry/" + LocalDate.now() + "/" + uuid + "_" + filename;

		return storeObjectPort.storeObject(image, objectKey);
	}

	private String generateFileName(String originalFilename) {
		String extension = "";
		int extensionIndex = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
		if (extensionIndex >= 0) {
			extension = originalFilename.substring(extensionIndex);
		}
		return UUID.randomUUID() + extension;
	}
}
