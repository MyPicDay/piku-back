package com.pikume.back.support.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.support.application.dto.InquiryAttachment;
import com.pikume.back.support.application.dto.SubmitInquiryCommand;
import com.pikume.back.support.application.port.in.SubmitInquiryUseCase;

import java.io.IOException;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
@Tag(name = "inquiry", description = "문의")
public class InquiryController {

	private final SubmitInquiryUseCase submitInquiryUseCase;

	@Operation(summary = "문의 작성", description = "사용자가 문의 내용을 작성하고, 선택적으로 이미지를 업로드합니다.  \n " +
			"이미지는 선택이며, 문의 내용은 최대 1000자까지 입력할 수 있습니다.")
	@SecurityRequirement(name = "JWT")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "문의 접수 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "유효하지 않은 문의 요청",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(
					responseCode = "404",
					description = "문의 제출 사용자 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(
					responseCode = "500",
					description = "첨부 파일 또는 서버 처리 실패",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> saveInquiry(
			@RequestPart("content") @Valid @Size(max = 1000) String content,
			@RequestPart(value = "image", required = false) MultipartFile image,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
		String userId = customUserDetails.getId();

		submitInquiryUseCase.submitInquiry(new SubmitInquiryCommand(
				userId,
				content,
				toAttachment(image)));
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	private InquiryAttachment toAttachment(MultipartFile image) throws IOException {
		if (image == null) {
			return null;
		}
		return new InquiryAttachment(image.getOriginalFilename(), image.getContentType(), image.getBytes());
	}
}
