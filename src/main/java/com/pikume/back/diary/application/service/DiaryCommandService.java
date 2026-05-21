package com.pikume.back.diary.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryCreatedResult;
import com.pikume.back.diary.application.dto.DiaryImageCommand;
import com.pikume.back.diary.application.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.application.exception.DiaryNotFoundException;
import com.pikume.back.diary.application.exception.DuplicateDiaryException;
import com.pikume.back.diary.application.port.in.CreateDiaryUseCase;
import com.pikume.back.diary.application.port.in.DeleteDiaryUseCase;
import com.pikume.back.diary.application.port.out.*;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.social.application.port.in.FriendUseCase;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
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
	public DiaryCreatedResult createDiary(CreateDiaryCommand diaryCommand, List<UploadedFileData> photos, String userId,
			RequestMetaInfo requestMetaInfo) throws IOException {

		validateDiaryCommand(diaryCommand, photos, userId);

		Diary diary = new Diary(diaryCommand.content(), diaryCommand.status(), diaryCommand.date(), userId);
		diary = saveDiaryPort.save(diary);
		log.debug("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());

		List<DiaryImageCommand> infos = new ArrayList<>(diaryCommand.imageInfos());
		infos.sort(Comparator.comparing(DiaryImageCommand::order));

		for (DiaryImageCommand info : infos) {
			if (info.type() == DiaryPhotoType.AI_IMAGE) {
				saveAiPhoto(diary, info.aiPhotoId(), userId, info.order());
			} else {
				if (info.photoIndex() != null && photos != null && info.photoIndex() < photos.size()) {
					photoStoragePort.savePhoto(diary, photos.get(info.photoIndex()), userId, info.order());
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

		return new DiaryCreatedResult(diary.getId(), diary.getContent());
	}

	public void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order) {
		log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());

		if (aiPhoto != null) {
			var diaryImageGeneration = loadCreativePort.findById(aiPhoto);
			String filePath = diaryImageGeneration.filePath();

			boolean isRepresent = (order != null && order == 0);
			if (isRepresent) {
				String oldPath = filePath;
				filePath = photoStoragePort.moveToPublic(filePath);
				log.info("대표 사진을 public 경로로 이동 완료: {} → {}", oldPath, filePath);

				loadCreativePort.updateFilePath(aiPhoto, filePath);
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

	private void validateDiaryDate(CreateDiaryCommand diaryCommand, String userId) {
		Optional<Diary> existingDiary = loadDiaryPort.findByUserIdAndDate(userId, diaryCommand.date());
        if (existingDiary.isPresent()) {
            log.info("일기 날짜 중복 요청");
            throw new DuplicateDiaryException(diaryCommand.date());
        }
        LocalDate localDate = LocalDate.now();
		if (diaryCommand.date().isAfter(localDate)) {
			log.error("미래 날짜에 일기 작성 시도: {}", diaryCommand.date());
			throw new IllegalArgumentException("미래 날짜에 일기를 작성할 수 없습니다: " + diaryCommand.date());
		}
	}

	private void validateDiaryCommand(CreateDiaryCommand diaryCommand, List<UploadedFileData> photos, String userId) {
		validatePhotos(photos);
		validateDiaryDate(diaryCommand, userId);

		List<DiaryImageCommand> infos = new ArrayList<>(diaryCommand.imageInfos());
		infos.sort(Comparator.comparing(DiaryImageCommand::order));
		Set<Integer> uniqueOrders = infos.stream()
				.map(DiaryImageCommand::order)
				.collect(Collectors.toSet());
		if (uniqueOrders.size() != infos.size()) {
			throw new IllegalArgumentException("이미지 순서가 중복되었습니다.");
		}
		int userImageCount = 0;
		for (DiaryImageCommand info : infos) {
			if (info.type() == DiaryPhotoType.AI_IMAGE) {
				if (!loadCreativePort.existsByIdAndUserId(info.aiPhotoId(), userId)) {
					throw new IllegalArgumentException("유효하지 않은 AI 사진 ID: " + info.aiPhotoId());
				}
			}
			if (info.type() == DiaryPhotoType.USER_IMAGE) {
				if (info.photoIndex() == null) {
					throw new IllegalArgumentException("유효하지 않은 사용자 사진 인덱스: null");
				}
				userImageCount++;
			}
		}
		if (userImageCount != (photos == null ? 0 : photos.size())) {
			throw new IllegalArgumentException("사용자 사진 개수와 이미지 정보 개수가 일치하지 않습니다.");
		}
	}

	private void validatePhotos(List<UploadedFileData> photos) {
		if (photos == null || photos.isEmpty()) {
			return;
		}
		for (UploadedFileData file : photos) {
			String originalFilename = file.originalFilename();

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
