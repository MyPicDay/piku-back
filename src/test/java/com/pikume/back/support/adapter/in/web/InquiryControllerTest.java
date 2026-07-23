package com.pikume.back.support.adapter.in.web;

import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.support.application.dto.SubmitInquiryCommand;
import com.pikume.back.support.application.port.in.SubmitInquiryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryController")
class InquiryControllerTest {

	@Mock
	private SubmitInquiryUseCase submitInquiryUseCase;

	@Test
	@DisplayName("기존 Multipart 필드를 Support 명령으로 변환하고 201 빈 응답을 반환한다")
	void mapsMultipartRequestToCommand() throws Exception {
		InquiryController controller = new InquiryController(submitInquiryUseCase);
		MockMultipartFile image =
				new MockMultipartFile("image", "inquiry.png", "image/png", "data".getBytes());

		var response = controller.saveInquiry(
				"문의 내용",
				image,
				new UserPrincipal("user-1", "nickname"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNull();
		ArgumentCaptor<SubmitInquiryCommand> commandCaptor =
				ArgumentCaptor.forClass(SubmitInquiryCommand.class);
		then(submitInquiryUseCase).should().submitInquiry(commandCaptor.capture());
		assertThat(commandCaptor.getValue().userId()).isEqualTo("user-1");
		assertThat(commandCaptor.getValue().content()).isEqualTo("문의 내용");
		assertThat(commandCaptor.getValue().attachment().originalFilename()).isEqualTo("inquiry.png");
		assertThat(commandCaptor.getValue().attachment().bytes()).containsExactly("data".getBytes());
	}
}
