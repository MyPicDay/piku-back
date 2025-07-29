package store.piku.back.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.notification.dto.request.FcmTokenRequest;
import store.piku.back.notification.service.FcmTokenService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
@Tag(name = "Fcm", description = "Fcm 관리 API")
public class FcmController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<Void> saveToken(@RequestBody FcmTokenRequest request) {
        fcmTokenService.saveToken(request.getUserId(), request.getToken());
        return ResponseEntity.ok().build();
    }

}
