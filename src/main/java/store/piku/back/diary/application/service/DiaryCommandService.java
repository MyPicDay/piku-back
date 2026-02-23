package store.piku.back.diary.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.diary.adapter.in.web.dto.DiaryDTO;
import store.piku.back.diary.adapter.in.web.dto.DiaryImageInfo;
import store.piku.back.diary.adapter.in.web.dto.ResponseDiaryDTO;
import store.piku.back.diary.application.port.in.CreateDiaryUseCase;
import store.piku.back.diary.application.port.in.DeleteDiaryUseCase;
import store.piku.back.diary.application.port.out.*;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;
import store.piku.back.diary.domain.vo.DiaryPhotoType;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.diary.exception.DiaryAccessDeniedException;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.diary.exception.DuplicateDiaryException;
import store.piku.back.file.FileUtil;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import store.piku.back.social.application.port.in.FriendUseCase;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryCommandService implements CreateDiaryUseCase, DeleteDiaryUseCase {

	private final LoadDiaryPort loadDiaryPort;
	private final SaveDiaryPort saveDiaryPort;
	private final PhotoStoragePort photoStoragePort;
	private final LoadCreativePort loadCreativePort;
	private final LoadUserForDiaryPort loadUserForDiaryPort;
	private final SendDiaryNotificationPort sendDiaryNotificationPort;
	private final FriendUseCase friendUseCase;
	private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;
	private final FileUtil fileUtil;

	@Override
	@Transactional
	public void deleteDiary(Long diaryId, String userId) {
		Diary diary = loadDiaryPort.findById(diaryId)
				.orElseThrow(() -> {
					log.error("일기 ID [{}]에 해당하는 일기를 찾을 수 없습니다.", diaryId);
					return new DiaryNotFoundException();
				});

		if (!diary.isOwner(userId)) {
			log.error("사용자 [{}]는 일기 ID [{}]의 소유자가 아닙니다.", userId, diaryId);
			throw new DiaryAccessDeniedException();
		}

		diary.delete();
		saveDiaryPort.save(diary);
		log.info("사용자 [{}] - 일기 ID [{}] 삭제 완료", userId, diaryId);
	}

	@Override
	@Transactional
	public ResponseDiaryDTO createDiary(DiaryDTO diaryDTO, List<MultipartFile> photos, String userId,
			RequestMetaInfo requestMetaInfo) throws IOException {

		validateDiaryDTO(diaryDTO, photos, userId);

		Diary diary = new Diary(diaryDTO.getContent(), diaryDTO.getStatus(), diaryDTO.getDate(), userId);
		diary = saveDiaryPort.save(diary);
		log.debug("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());

		List<DiaryImageInfo> infos = diaryDTO.getImageInfos();
		infos.sort(Comparator.comparing(DiaryImageInfo::getOrder));

		for (DiaryImageInfo info : infos) {
			if (info.getType() == DiaryPhotoType.AI_IMAGE) {
				saveAiPhoto(diary, info.getAiPhotoId(), userId, info.getOrder());
			} else {
				if (info.getPhotoIndex() != null && photos != null && info.getPhotoIndex() < photos.size()) {
					photoStoragePort.savePhoto(diary, photos.get(info.getPhotoIndex()), userId, info.getOrder());
				}
			}
		}
		log.debug("사용자 [{}] - 사진 저장 완료. 일기 ID: {}", userId, diary.getId());

		if (diary.getStatus() == DiaryVisibility.FRIENDS) {
			List<String> friends = friendUseCase.getFriends(userId);
			sendDiaryNotificationPort.notifyFriendsOfNewDiary(friends, userId, diary, requestMetaInfo);
			log.info("친구에게 새 일기 공개 알림 전송 완료. 친구 수: {}", friends.size());
		}

		try {
			analyzeDiaryContentUseCase.analyzeAndSave(diary.getId(), diary.getContent());
			log.debug("일기 메타데이터 분석 완료 - diaryId: {}", diary.getId());
		} catch (Exception e) {
			log.warn("일기 메타데이터 분석 실패 - diaryId: {}, error: {}", diary.getId(), e.getMessage());
		}

		return new ResponseDiaryDTO(diary.getId(), diary.getContent());
	}

	public void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order) {
		log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());

		if (aiPhoto != null) {
			DiaryImageGeneration diaryImageGeneration = loadCreativePort.findById(aiPhoto);
			String filePath = diaryImageGeneration.getFilePath();

			boolean isRepresent = (order != null && order == 0);
			if (isRepresent) {
				String oldPath = filePath;
				filePath = photoStoragePort.moveToPublic(filePath);
				log.info("대표 사진을 public 경로로 이동 완료: {} → {}", oldPath, filePath);

				diaryImageGeneration.updateFilePath(filePath);
				log.info("DiaryImageGeneration filePath 업데이트 완료 (ID: {})", aiPhoto);
			}

			Photo savePhoto = new Photo(diary, filePath, order);
			if (isRepresent) {
				savePhoto.updateRepresent(true);
			}
			saveDiaryPort.savePhoto(savePhoto);
			loadCreativePort.updateDiaryId(aiPhoto, diary.getId());

			log.info("AI 사진 저장 완료 - 경로: {}, 대표사진: {}", filePath, isRepresent);
		} else {
			log.warn("빈 AI 사진 ID 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
		}
	}

	private void validateDiaryDate(DiaryDTO diaryDTO, String userId) {
		Optional<Diary> existingDiary = loadDiaryPort.findByUserIdAndDate(userId, diaryDTO.getDate());
		if (existingDiary.isPresent()) {
			log.info("일기 날짜 중복 요청");
			throw new DuplicateDiaryException("이미 해당 날짜에 일기가 존재합니다: " + diaryDTO.getDate());
		}
		LocalDate localDate = LocalDate.now();
		if (diaryDTO.getDate().isAfter(localDate)) {
			log.error("미래 날짜에 일기 작성 시도: {}", diaryDTO.getDate());
			throw new IllegalArgumentException("미래 날짜에 일기를 작성할 수 없습니다: " + diaryDTO.getDate());
		}
	}

	private void validateDiaryDTO(DiaryDTO diaryDTO, List<MultipartFile> photos, String userId) {
		validatePhotos(photos);
		validateDiaryDate(diaryDTO, userId);

		List<DiaryImageInfo> infos = diaryDTO.getImageInfos();
		infos.sort(Comparator.comparing(DiaryImageInfo::getOrder));
		Set<Integer> uniqueOrders = infos.stream()
				.map(DiaryImageInfo::getOrder)
				.collect(Collectors.toSet());
		if (uniqueOrders.size() != infos.size()) {
			throw new IllegalArgumentException("이미지 순서가 중복되었습니다.");
		}
		int userImageCount = 0;
		for (DiaryImageInfo info : infos) {
			if (info.getType() == DiaryPhotoType.AI_IMAGE) {
				if (!loadCreativePort.existsByIdAndUserId(info.getAiPhotoId(), userId)) {
					throw new IllegalArgumentException("유효하지 않은 AI 사진 ID: " + info.getAiPhotoId());
				}
			}
			if (info.getType() == DiaryPhotoType.USER_IMAGE) {
				if (info.getPhotoIndex() == null) {
					throw new IllegalArgumentException("유효하지 않은 사용자 사진 인덱스: null");
				}
				userImageCount++;
			}
		}
		if (userImageCount != (photos == null ? 0 : photos.size())) {
			throw new IllegalArgumentException("사용자 사진 개수와 이미지 정보 개수가 일치하지 않습니다.");
		}
	}

	private void validatePhotos(List<MultipartFile> photos) {
		if (photos == null || photos.isEmpty()) {
			return;
		}
		for (MultipartFile file : photos) {
			String originalFilename = file.getOriginalFilename();

			if (originalFilename == null || !originalFilename.contains(".")) {
				throw new IllegalArgumentException("유효하지 않은 파일 이름입니다: " + originalFilename);
			}

			String contentType = fileUtil.getContentType(originalFilename);

			List<String> allowedImageTypes = List.of(
					"image/jpeg",
					"image/png",
					"image/gif",
					"image/webp",
					"image/bmp",
					"image/svg+xml");

			if (!allowedImageTypes.contains(contentType)) {
				throw new IllegalArgumentException("허용되지 않는 이미지 확장자입니다: " + originalFilename);
			}
		}
	}
}
