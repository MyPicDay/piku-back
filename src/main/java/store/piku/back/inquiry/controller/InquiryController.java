package store.piku.back.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.inquiry.service.InquiryService;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
@Tag(name = "inquiry", description = "문의")
public class InquiryController {


    private final InquiryService inquiryService;

    @Operation(
            summary = "문의 작성",
            description = "사용자가 문의 내용을 작성하고, 선택적으로 이미지를 업로드합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveInquiry(
            @RequestPart String content,
            @RequestPart(required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String userId = customUserDetails.getId();

        try {
            inquiryService.saveInquiry(userId, content, image);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
