package store.piku.back.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.ai.dto.AiDiaryResponseDTO;
import store.piku.back.ai.service.ImageGenerationService;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {

    private final ImageGenerationService imageGenerationService;
    private final RequestMetaMapper requestMetaMapper;

    @PostMapping("/diary/ai/generate")
    public ResponseEntity<AiDiaryResponseDTO> generateDiaryImage(@RequestBody Map<String, String> body, HttpServletRequest request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String content = body.get("content");
        String userId = customUserDetails.getId();

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        AiDiaryResponseDTO dto = imageGenerationService.diaryImage(content, userId, requestMetaInfo);
        log.info("Generated image URL: {}", dto.getUrl());
        return ResponseEntity.ok(dto);
    }
}
