package com.pikume.back.support.application.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface UploadInquiryImagePort {

	/**
	 * 문의 이미지를 업로드하고 URL을 반환합니다.
	 *
	 * @param image  업로드할 이미지 파일
	 * @param userId 사용자 ID
	 * @return 업로드된 이미지 URL
	 */
	String upload(MultipartFile image, String userId);
}
