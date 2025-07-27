package store.piku.back.inquiry.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.auth.service.EmailService;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.diary.service.PhotoUtil;
import store.piku.back.inquiry.entity.Inquiry;
import store.piku.back.inquiry.repository.InquiryRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    private final PhotoStorageService photoStorageService;
    private final InquiryRepository inquiryRepository;
    private final UserReader userReader;
    private final PhotoUtil photoUtil;
    private final EmailService emailService;

    public void saveInquiry(String userId, String content, MultipartFile image) {
        User user = userReader.getUserById(userId);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String originalFilename = image.getOriginalFilename();
            String filename = photoUtil.generateFileName(LocalDate.now(), Objects.requireNonNull(originalFilename));
            String UUID = userId.substring(0, 8);
            String objectKey = "inquiry/"+ LocalDate.now() + "/" + UUID +"_" +filename;

            imageUrl = photoStorageService.uploadToStorage(image, userId,objectKey);
        }

        try{
            emailService.sendFeedbackEmail(content, image);
        }catch (MessagingException | UnsupportedEncodingException e) {
            log.error("피드백 이메일 전송 실패: {}", e.getMessage());
        }

        Inquiry inquiry = new Inquiry(user, content, imageUrl);
        inquiryRepository.save(inquiry);
    }
}

