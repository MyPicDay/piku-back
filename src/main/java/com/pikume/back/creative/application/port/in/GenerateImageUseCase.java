package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.dto.GenerateDiaryImageCommand;

/**
 * 이미지 생성 Inbound Port
 */
public interface GenerateImageUseCase {

	/**
	 * 일기 내용 기반 AI 이미지 생성
	 * 
	 * @return 생성 결과 (id, url 포함)
	 */
	GeneratedImageResult generateDiaryImage(GenerateDiaryImageCommand command);
}
