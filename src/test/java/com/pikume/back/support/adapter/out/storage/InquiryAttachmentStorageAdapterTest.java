package com.pikume.back.support.adapter.out.storage;

import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.StoreObjectPort;
import com.pikume.back.support.application.dto.InquiryAttachment;
import com.pikume.back.support.application.exception.SupportErrorCode;
import com.pikume.back.support.application.exception.SupportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryAttachmentStorageAdapter")
class InquiryAttachmentStorageAdapterTest {

	@Mock
	private StoreObjectPort storeObjectPort;

	@Test
	@DisplayName("Support 첨부를 중립 Object Storage 계약으로 저장한다")
	void storesAttachmentThroughNeutralObjectPort() {
		given(storeObjectPort.storeObject(any(UploadedFileData.class), any(String.class)))
				.willAnswer(invocation -> invocation.getArgument(1));
		InquiryAttachmentStorageAdapter adapter = adapter();

		String reference = adapter.storeInquiryAttachment(
				new InquiryAttachment("inquiry.png", "image/png", "data".getBytes()),
				"550e8400-e29b-41d4-a716-446655440000");

		assertThat(reference).startsWith("inquiry/");
		then(storeObjectPort).should().storeObject(
				org.mockito.ArgumentMatchers.argThat(file ->
						file.originalFilename().equals("inquiry.png")
								&& file.contentType().equals("image/png")
								&& java.util.Arrays.equals(file.bytes(), "data".getBytes())),
				org.mockito.ArgumentMatchers.eq(reference));
	}

	@Test
	@DisplayName("Storage 원인을 노출하지 않는 Support 오류로 변환한다")
	void translatesStorageFailure() {
		given(storeObjectPort.storeObject(any(UploadedFileData.class), any(String.class)))
				.willThrow(new IllegalStateException("bucket secret"));

		assertThatThrownBy(() -> adapter().storeInquiryAttachment(
				new InquiryAttachment("inquiry.png", "image/png", "data".getBytes()),
				"user-1"))
				.isInstanceOfSatisfying(SupportException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(SupportErrorCode.ATTACHMENT_STORAGE_FAILED);
					assertThat(exception.getMessage()).doesNotContain("bucket secret");
				});
	}

	private InquiryAttachmentStorageAdapter adapter() {
		return new InquiryAttachmentStorageAdapter(
				storeObjectPort,
				new InquiryAttachmentObjectKeyPolicy());
	}
}
