package com.pikume.back.support.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.support.application.port.out.LoadUserInfoForSupportPort;
import com.pikume.back.support.application.port.out.SaveInquiryPort;
import com.pikume.back.support.application.port.out.SendFeedbackEmailPort;
import com.pikume.back.support.application.port.out.UploadInquiryImagePort;
import com.pikume.back.support.domain.Inquiry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

	@InjectMocks
	private InquiryService inquiryService;

	@Mock
	private LoadUserInfoForSupportPort loadUserInfoForSupportPort;

	@Mock
	private UploadInquiryImagePort uploadInquiryImagePort;

	@Mock
	private SendFeedbackEmailPort sendFeedbackEmailPort;

	@Mock
	private SaveInquiryPort saveInquiryPort;

	@Nested
	@DisplayName("submitInquiry - 문의 등록")
	class SubmitInquiry {

		@Test
		@DisplayName("이미지 포함 문의를 성공적으로 등록한다")
		void submitWithImage() {
			UploadedFileData mockImage = new UploadedFileData("inquiry.png", "image/png", "data".getBytes());
			given(loadUserInfoForSupportPort.existsById("user-id")).willReturn(true);
			given(uploadInquiryImagePort.upload(mockImage, "user-id")).willReturn("https://image-url.com/img.jpg");

			inquiryService.submitInquiry("user-id", "문의 내용입니다", mockImage);

			then(uploadInquiryImagePort).should().upload(mockImage, "user-id");
			then(sendFeedbackEmailPort).should().sendFeedbackEmail("문의 내용입니다", mockImage);
			then(saveInquiryPort).should().save(any(Inquiry.class));
		}

		@Test
		@DisplayName("이미지 없이 문의를 성공적으로 등록한다")
		void submitWithoutImage() {
			given(loadUserInfoForSupportPort.existsById("user-id")).willReturn(true);

			inquiryService.submitInquiry("user-id", "문의 내용입니다", null);

			then(uploadInquiryImagePort).should(never()).upload(any(), any());
			then(sendFeedbackEmailPort).should().sendFeedbackEmail("문의 내용입니다", null);
			then(saveInquiryPort).should().save(any(Inquiry.class));
		}

		@Test
		@DisplayName("존재하지 않는 사용자가 문의하면 예외 발생")
		void failsUserNotFound() {
			given(loadUserInfoForSupportPort.existsById("ghost-id")).willReturn(false);

			assertThatThrownBy(() -> inquiryService.submitInquiry("ghost-id", "문의", null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("존재하지 않는 사용자");
		}

		@Test
		@DisplayName("이메일 전송 실패 시에도 문의 저장은 성공한다")
		void emailFailureDoesNotBlockSave() {
			UploadedFileData mockImage = new UploadedFileData("inquiry.png", "image/png", "data".getBytes());
			given(loadUserInfoForSupportPort.existsById("user-id")).willReturn(true);
			given(uploadInquiryImagePort.upload(mockImage, "user-id")).willReturn("https://img.jpg");
			willThrow(new RuntimeException("SMTP 오류"))
					.given(sendFeedbackEmailPort).sendFeedbackEmail(any(), any());

			inquiryService.submitInquiry("user-id", "문의 내용", mockImage);

			then(saveInquiryPort).should().save(any(Inquiry.class));
		}
	}
}
