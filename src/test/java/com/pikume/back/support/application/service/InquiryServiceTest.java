package com.pikume.back.support.application.service;

import com.pikume.back.support.application.dto.InquiryAttachment;
import com.pikume.back.support.application.dto.SubmitInquiryCommand;
import com.pikume.back.support.application.exception.SupportErrorCode;
import com.pikume.back.support.application.exception.SupportException;
import com.pikume.back.support.application.port.out.RecordInquiryPort;
import com.pikume.back.support.application.port.out.SendInquiryNotificationPort;
import com.pikume.back.support.application.port.out.StoreInquiryAttachmentPort;
import com.pikume.back.support.application.port.out.VerifyInquirySubmitterPort;
import com.pikume.back.support.domain.Inquiry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryService")
class InquiryServiceTest {

	@Mock
	private VerifyInquirySubmitterPort verifyInquirySubmitterPort;
	@Mock
	private StoreInquiryAttachmentPort storeInquiryAttachmentPort;
	@Mock
	private SendInquiryNotificationPort sendInquiryNotificationPort;
	@Mock
	private RecordInquiryPort recordInquiryPort;

	private InquiryService inquiryService;

	@BeforeEach
	void setUp() {
		inquiryService = new InquiryService(
				verifyInquirySubmitterPort,
				storeInquiryAttachmentPort,
				sendInquiryNotificationPort,
				recordInquiryPort);
	}

	@Nested
	@DisplayName("submitInquiry")
	class SubmitInquiry {

		@Test
		@DisplayName("현재 사용자 확인, 첨부 저장, 메일 시도, 문의 기록 순서를 유지한다")
		void preservesCurrentSubmissionOrder() {
			InquiryAttachment attachment = attachment();
			given(verifyInquirySubmitterPort.inquirySubmitterExists("user-id")).willReturn(true);
			given(storeInquiryAttachmentPort.storeInquiryAttachment(attachment, "user-id"))
					.willReturn("inquiry/path/image.png");

			inquiryService.submitInquiry(new SubmitInquiryCommand(
					"user-id",
					"문의 내용입니다",
					attachment));

			InOrder order = inOrder(
					verifyInquirySubmitterPort,
					storeInquiryAttachmentPort,
					sendInquiryNotificationPort,
					recordInquiryPort);
			order.verify(verifyInquirySubmitterPort).inquirySubmitterExists("user-id");
			order.verify(storeInquiryAttachmentPort).storeInquiryAttachment(attachment, "user-id");
			order.verify(sendInquiryNotificationPort)
					.sendInquiryNotification("문의 내용입니다", attachment);
			order.verify(recordInquiryPort).recordInquiry(
					org.mockito.ArgumentMatchers.argThat(inquiry ->
							inquiry.getAttachmentReference().equals("inquiry/path/image.png")));
		}

		@Test
		@DisplayName("첨부가 없으면 Storage를 호출하지 않고 메일과 문의 기록을 수행한다")
		void submitsWithoutAttachment() {
			given(verifyInquirySubmitterPort.inquirySubmitterExists("user-id")).willReturn(true);

			inquiryService.submitInquiry(new SubmitInquiryCommand(
					"user-id",
					"문의 내용입니다",
					null));

			then(storeInquiryAttachmentPort).should(never())
					.storeInquiryAttachment(any(), any());
			then(sendInquiryNotificationPort).should()
					.sendInquiryNotification("문의 내용입니다", null);
			then(recordInquiryPort).should().recordInquiry(any(Inquiry.class));
		}

		@Test
		@DisplayName("존재하지 않는 제출자는 기술 중립 Support 오류로 거부한다")
		void rejectsMissingSubmitter() {
			given(verifyInquirySubmitterPort.inquirySubmitterExists("ghost-id")).willReturn(false);

			assertThatThrownBy(() -> inquiryService.submitInquiry(
					new SubmitInquiryCommand("ghost-id", "문의", null)))
					.isInstanceOfSatisfying(SupportException.class,
							exception -> assertThat(exception.getErrorCode())
									.isEqualTo(SupportErrorCode.SUBMITTER_NOT_FOUND));

			then(sendInquiryNotificationPort).shouldHaveNoInteractions();
			then(recordInquiryPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("메일 실패는 현재처럼 무시하고 문의를 기록한다")
		void notificationFailureDoesNotBlockRecording() {
			InquiryAttachment attachment = attachment();
			given(verifyInquirySubmitterPort.inquirySubmitterExists("user-id")).willReturn(true);
			given(storeInquiryAttachmentPort.storeInquiryAttachment(attachment, "user-id"))
					.willReturn("inquiry/path/image.png");
			willThrow(new IllegalStateException("smtp secret failure"))
					.given(sendInquiryNotificationPort)
					.sendInquiryNotification("문의 내용", attachment);

			inquiryService.submitInquiry(new SubmitInquiryCommand(
					"user-id",
					"문의 내용",
					attachment));

			then(recordInquiryPort).should().recordInquiry(any(Inquiry.class));
		}

		@Test
		@DisplayName("첨부 저장 실패는 Support 저장 오류로 변환하고 메일과 문의 기록을 중단한다")
		void attachmentStorageFailureStopsSubmission() {
			InquiryAttachment attachment = attachment();
			given(verifyInquirySubmitterPort.inquirySubmitterExists("user-id")).willReturn(true);
			given(storeInquiryAttachmentPort.storeInquiryAttachment(attachment, "user-id"))
					.willThrow(new IllegalStateException("storage secret failure"));

			assertThatThrownBy(() -> inquiryService.submitInquiry(
					new SubmitInquiryCommand("user-id", "문의", attachment)))
					.isInstanceOfSatisfying(SupportException.class, exception -> {
						assertThat(exception.getErrorCode())
								.isEqualTo(SupportErrorCode.ATTACHMENT_STORAGE_FAILED);
						assertThat(exception.getMessage()).doesNotContain("storage secret failure");
					});

			then(sendInquiryNotificationPort).shouldHaveNoInteractions();
			then(recordInquiryPort).shouldHaveNoInteractions();
		}
	}

	private InquiryAttachment attachment() {
		return new InquiryAttachment("inquiry.png", "image/png", "data".getBytes());
	}
}
