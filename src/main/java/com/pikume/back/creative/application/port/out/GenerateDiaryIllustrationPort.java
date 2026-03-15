package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.application.dto.DiaryIllustrationRequest;
import com.pikume.back.creative.application.dto.GeneratedIllustrationPayload;

public interface GenerateDiaryIllustrationPort {

	GeneratedIllustrationPayload generate(DiaryIllustrationRequest request);
}
