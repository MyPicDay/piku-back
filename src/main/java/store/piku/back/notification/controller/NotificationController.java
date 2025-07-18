package store.piku.back.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.notification.service.NotificationService;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
//    private


}
