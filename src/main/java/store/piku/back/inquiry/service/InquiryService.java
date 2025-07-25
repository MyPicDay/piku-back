package store.piku.back.inquiry.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.diary.service.PhotoUtil;
import store.piku.back.inquiry.entity.Inquiry;
import store.piku.back.inquiry.repository.InquiryRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InquiryService {


    private final PhotoStorageService photoStorageService;
    private final InquiryRepository inquiryRepository;
    private final UserReader userReader;
    private final PhotoUtil photoUtil;

    public void saveInquiry(String userId, String content, MultipartFile image) {
        User user = userReader.getUserById(userId);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String originalFilename = image.getOriginalFilename();
            String filename = photoUtil.generateFileName(LocalDate.now(), originalFilename);
            String UUID = userId.substring(0, 8);
            String objectKey = "inquiry/"+ LocalDate.now() + "/" + UUID +"_" +filename;

            imageUrl = photoStorageService.uploadToStorage(image, userId,objectKey);
        }

        Inquiry inquiry = new Inquiry(user, content, imageUrl);
        inquiryRepository.save(inquiry);
    }
}

