package store.piku.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.diary.service.PhotoUtil;
import store.piku.back.support.application.port.out.UploadInquiryImagePort;

import java.time.LocalDate;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InquiryImageUploadAdapter implements UploadInquiryImagePort {

	private final PhotoStorageService photoStorageService;
	private final PhotoUtil photoUtil;

	@Override
	public String upload(MultipartFile image, String userId) {
		String originalFilename = image.getOriginalFilename();
		String filename = photoUtil.generateFileName(LocalDate.now(), Objects.requireNonNull(originalFilename));
		String uuid = userId.substring(0, 8);
		String objectKey = "inquiry/" + LocalDate.now() + "/" + uuid + "_" + filename;

		return photoStorageService.uploadToStorage(image, userId, objectKey);
	}
}
