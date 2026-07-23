package com.pikume.back.support.adapter.out.storage;

import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.StoreObjectPort;
import com.pikume.back.support.application.dto.InquiryAttachment;
import com.pikume.back.support.application.exception.SupportErrorCode;
import com.pikume.back.support.application.exception.SupportException;
import com.pikume.back.support.application.port.out.StoreInquiryAttachmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class InquiryAttachmentStorageAdapter implements StoreInquiryAttachmentPort {

	private final StoreObjectPort storeObjectPort;
	private final InquiryAttachmentObjectKeyPolicy objectKeyPolicy;

	@Override
	public String storeInquiryAttachment(InquiryAttachment attachment, String userId) {
		try {
			String objectKey = objectKeyPolicy.createObjectKey(
					userId,
					attachment.originalFilename(),
					LocalDate.now());
			UploadedFileData file = new UploadedFileData(
					attachment.originalFilename(),
					attachment.contentType(),
					attachment.bytes());
			return storeObjectPort.storeObject(file, objectKey, null);
		} catch (RuntimeException exception) {
			throw new SupportException(SupportErrorCode.ATTACHMENT_STORAGE_FAILED, exception);
		}
	}
}
