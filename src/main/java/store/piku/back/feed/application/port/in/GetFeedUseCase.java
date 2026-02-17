package store.piku.back.feed.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.diary.adapter.in.web.dto.ResponseDTO;
import store.piku.back.global.dto.RequestMetaInfo;

public interface GetFeedUseCase {

	ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId);

	Page<ResponseDTO> getAllDiaries(Pageable pageable, RequestMetaInfo requestMetaInfo, String userId);

	void logClick(String userId, Long diaryId);
}
