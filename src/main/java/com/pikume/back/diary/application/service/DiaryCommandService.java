package com.pikume.back.diary.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryCreatedResult;
import com.pikume.back.diary.application.dto.DiaryImageCommand;
import com.pikume.back.diary.application.dto.DiaryUpdatedResult;
import com.pikume.back.diary.application.dto.UpdateDiaryCommand;
import com.pikume.back.diary.application.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.application.exception.DiaryImageRelocationException;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.exception.DiaryNotFoundException;
import com.pikume.back.diary.application.exception.DuplicateDiaryException;
import com.pikume.back.diary.application.port.in.CreateDiaryUseCase;
import com.pikume.back.diary.application.port.in.DeleteDiaryUseCase;
import com.pikume.back.diary.application.port.in.UpdateDiaryUseCase;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryCommandService implements CreateDiaryUseCase, DeleteDiaryUseCase, UpdateDiaryUseCase {

	private final LoadDiaryPort loadDiaryPort;
	private final SaveDiaryPort saveDiaryPort;
	private final PhotoStoragePort photoStoragePort;
	private final ManageGeneratedImageForDiaryPort manageGeneratedImageForDiaryPort;
	private final DeleteDiaryNotificationPort deleteDiaryNotificationPort;
	private final SendDiaryNotificationPort sendDiaryNotificationPort;
	private final FriendUseCase friendUseCase;
	private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;
	private final FileUtil fileUtil;

	@Override
	@Transactional
	public void deleteDiary(Long diaryId, String userId) {
		Diary diary = loadOwnedDiary(diaryId, userId);
		diary.delete();
		saveDiaryPort.save(diary);
		deleteDiaryNotificationPort.deleteNotificationsByDiaryId(diaryId);
		log.info("사용자 [{}] - 일기 ID [{}] 삭제 완료", userId, diaryId);
	}

	@Override
	@Transactional
	public DiaryUpdatedResult updateDiary(Long diaryId, UpdateDiaryCommand command, String userId) {
		if (command == null) {
			throw new DiaryInvalidRequestException("일기 수정 요청은 필수입니다.");
		}

		Diary diary = loadOwnedDiary(diaryId, userId);
		ImageRelocationResult relocationResult = ImageRelocationResult.empty();
		try {
			relocationResult = relocatePhotosForVisibilityChange(diary, command.status());
			diary.updateContentAndStatus(command.content(), command.status());
			Diary savedDiary = saveDiaryPort.save(diary);
			registerImageRelocationCleanup(relocationResult);
			log.info("사용자 [{}] - 일기 ID [{}] 수정 완료", userId, diaryId);
			analyzeDiaryMetadataAfterCommit(savedDiary);

			return new DiaryUpdatedResult(savedDiary.getId(), savedDiary.getStatus(), savedDiary.getContent());
		} catch (RuntimeException e) {
			deleteCopiedObjectsForRollback(relocationResult.copiedObjectKeys());
			throw e;
		}
	}

	private ImageRelocationResult relocatePhotosForVisibilityChange(Diary diary, DiaryVisibility targetVisibility) {
		if (isPublicDiary(diary.getStatus()) == isPublicDiary(targetVisibility)) {
			return ImageRelocationResult.empty();
		}

		List<Photo> photos = loadDiaryPort.findPhotosByDiaryIds(Set.of(diary.getId()));
		if (photos.isEmpty()) {
			return ImageRelocationResult.empty();
		}

		List<PhotoScopeTransition> transitions = new ArrayList<>();
		List<String> copiedObjectKeys = new ArrayList<>();
		try {
			for (Photo photo : photos) {
				PhotoScopeTransition transition = copyPhotoToVisibilityScope(photo, targetVisibility, copiedObjectKeys);
				if (transition.hasChanged()) {
					transitions.add(transition);
				}
			}

			for (PhotoScopeTransition transition : transitions) {
				transition.apply();
				saveDiaryPort.savePhoto(transition.photo());
			}

			return new ImageRelocationResult(
					List.copyOf(new LinkedHashSet<>(copiedObjectKeys)),
					oldObjectKeysToDelete(transitions));
		} catch (DiaryImageRelocationException e) {
			deleteCopiedObjectsForRollback(copiedObjectKeys);
			throw e;
		} catch (RuntimeException e) {
			deleteCopiedObjectsForRollback(copiedObjectKeys);
			throw new DiaryImageRelocationException("일기 이미지 공개범위 변경 중 오류가 발생했습니다.", e);
		}
	}

	private PhotoScopeTransition copyPhotoToVisibilityScope(
			Photo photo,
			DiaryVisibility targetVisibility,
			List<String> copiedObjectKeys) {
		String oldUrl = photo.getUrl();
		String newUrl = copyObjectToVisibilityScope(oldUrl, targetVisibility, photo.getSourceType(), copiedObjectKeys);

		String oldOptimizedUrl = photo.getOptimizedUrl();
		String newOptimizedUrl = oldOptimizedUrl;
		if (hasText(oldOptimizedUrl)) {
			if (Objects.equals(oldOptimizedUrl, oldUrl)) {
				newOptimizedUrl = newUrl;
			} else {
				newOptimizedUrl = copyObjectToVisibilityScope(
						oldOptimizedUrl,
						targetVisibility,
						photo.getSourceType(),
						copiedObjectKeys);
			}
		}

		return new PhotoScopeTransition(photo, oldUrl, newUrl, oldOptimizedUrl, newOptimizedUrl);
	}

	private String copyObjectToVisibilityScope(
			String objectKey,
			DiaryVisibility targetVisibility,
			DiaryPhotoType sourceType,
			List<String> copiedObjectKeys) {
		if (!hasText(objectKey)) {
			return objectKey;
		}

		String copiedObjectKey = photoStoragePort.copyToVisibilityScope(objectKey, targetVisibility, sourceType);
		if (!Objects.equals(objectKey, copiedObjectKey)) {
			copiedObjectKeys.add(copiedObjectKey);
		}
		return copiedObjectKey;
	}

	private List<String> oldObjectKeysToDelete(List<PhotoScopeTransition> transitions) {
		LinkedHashSet<String> objectKeys = new LinkedHashSet<>();
		for (PhotoScopeTransition transition : transitions) {
			for (String oldObjectKey : transition.oldObjectKeys()) {
				if (hasText(oldObjectKey) && !transition.newObjectKeys().contains(oldObjectKey)) {
					objectKeys.add(oldObjectKey);
				}
			}
		}
		return List.copyOf(objectKeys);
	}

	private void registerImageRelocationCleanup(ImageRelocationResult relocationResult) {
		if (relocationResult.isEmpty()) {
			return;
		}

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteOldObjectsForAdminCleanup(relocationResult.oldObjectKeys());
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteOldObjectsForAdminCleanup(relocationResult.oldObjectKeys());
			}

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					deleteCopiedObjectsForRollback(relocationResult.copiedObjectKeys());
				}
			}
		});
	}

	private void deleteCopiedObjectsForRollback(List<String> objectKeys) {
		deleteObjects(objectKeys, "image_relocation_copied_object_cleanup_failed");
	}

	private void deleteOldObjectsForAdminCleanup(List<String> objectKeys) {
		deleteObjects(objectKeys, "image_relocation_old_object_cleanup_failed");
	}

	private void deleteObjects(List<String> objectKeys, String eventName) {
		if (objectKeys == null || objectKeys.isEmpty()) {
			return;
		}
		for (String objectKey : new LinkedHashSet<>(objectKeys)) {
			try {
				photoStoragePort.deleteObject(objectKey);
			} catch (RuntimeException e) {
				log.error("event={} outcome=failed objectKey={} fileName={} reason={}",
						eventName,
						objectKey,
						fileNameFromObjectKey(objectKey),
						e.getMessage(),
						e);
			}
		}
	}

	private String fileNameFromObjectKey(String objectKey) {
		if (!hasText(objectKey)) {
			return "";
		}
		int slashIndex = objectKey.lastIndexOf('/');
		if (slashIndex < 0 || slashIndex == objectKey.length() - 1) {
			return objectKey;
		}
		return objectKey.substring(slashIndex + 1);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private Diary loadOwnedDiary(Long diaryId, String userId) {
		Diary diary = loadDiaryPort.findById(diaryId)
				.orElseThrow(() -> {
					log.error("일기 ID [{}]에 해당하는 일기를 찾을 수 없습니다.", diaryId);
					return new DiaryNotFoundException();
				});

		if (!diary.isOwner(userId)) {
			log.error("사용자 [{}]는 일기 ID [{}]의 소유자가 아닙니다.", userId, diaryId);
			throw new DiaryAccessDeniedException();
		}

		return diary;
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

		analyzeDiaryMetadataAfterCommit(diary);

		return new DiaryCreatedResult(diary.getId(), diary.getContent());
	}

	private void analyzeDiaryMetadataAfterCommit(Diary diary) {
		Long diaryId = diary.getId();
		String content = diary.getContent();
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			analyzeDiaryMetadata(diaryId, content);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				analyzeDiaryMetadata(diaryId, content);
			}
		});
	}

	private void analyzeDiaryMetadata(Long diaryId, String content) {
		try {
			analyzeDiaryContentUseCase.analyzeAndSave(diaryId, content);
			log.debug("일기 메타데이터 분석 완료 - diaryId: {}", diaryId);
		} catch (Exception e) {
			log.warn("일기 메타데이터 분석 실패 - diaryId: {}, error: {}", diaryId, e.getMessage());
		}
	}

	public void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order) {
		log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());

		if (aiPhoto != null) {
			String filePath = manageGeneratedImageForDiaryPort.loadGeneratedImagePath(aiPhoto);

			if (isPublicDiary(diary.getStatus())) {
				String oldPath = filePath;
				filePath = photoStoragePort.moveToPublic(filePath);
				log.info("공개 일기 AI 사진을 public 경로로 이동 완료: {} → {}", oldPath, filePath);

				manageGeneratedImageForDiaryPort.updateGeneratedImagePath(aiPhoto, filePath);
				log.info("DiaryImageGeneration filePath 업데이트 완료 (ID: {})", aiPhoto);
			}

			boolean isRepresent = (order != null && order == 0);
			Photo savePhoto = new Photo(diary, filePath, order, DiaryPhotoType.AI_IMAGE);
			if (isRepresent) {
				savePhoto.updateRepresent(true);
			}
			saveDiaryPort.savePhoto(savePhoto);
			manageGeneratedImageForDiaryPort.attachGeneratedImageToDiary(aiPhoto, diary.getId());

			log.info("AI 사진 저장 완료 - 경로: {}, 대표사진: {}", filePath, isRepresent);
		} else {
			log.warn("빈 AI 사진 ID 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
		}
	}

	private boolean isPublicDiary(DiaryVisibility visibility) {
		return visibility == DiaryVisibility.PUBLIC || visibility == DiaryVisibility.ANONYMOUS;
	}

	private record ImageRelocationResult(List<String> copiedObjectKeys, List<String> oldObjectKeys) {

		private static ImageRelocationResult empty() {
			return new ImageRelocationResult(List.of(), List.of());
		}

		private boolean isEmpty() {
			return copiedObjectKeys.isEmpty() && oldObjectKeys.isEmpty();
		}
	}

	private record PhotoScopeTransition(
			Photo photo,
			String oldUrl,
			String newUrl,
			String oldOptimizedUrl,
			String newOptimizedUrl) {

		private boolean hasChanged() {
			return !Objects.equals(oldUrl, newUrl) || !Objects.equals(oldOptimizedUrl, newOptimizedUrl);
		}

		private void apply() {
			photo.updateObjectKeys(newUrl, newOptimizedUrl);
		}

		private List<String> oldObjectKeys() {
			LinkedHashSet<String> objectKeys = new LinkedHashSet<>();
			if (oldUrl != null && !oldUrl.isBlank()) {
				objectKeys.add(oldUrl);
			}
			if (oldOptimizedUrl != null && !oldOptimizedUrl.isBlank()) {
				objectKeys.add(oldOptimizedUrl);
			}
			return List.copyOf(objectKeys);
		}

		private List<String> newObjectKeys() {
			LinkedHashSet<String> objectKeys = new LinkedHashSet<>();
			if (newUrl != null && !newUrl.isBlank()) {
				objectKeys.add(newUrl);
			}
			if (newOptimizedUrl != null && !newOptimizedUrl.isBlank()) {
				objectKeys.add(newOptimizedUrl);
			}
			return List.copyOf(objectKeys);
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
			throw new DiaryInvalidRequestException("미래 날짜에 일기를 작성할 수 없습니다: " + diaryCommand.date());
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
			throw new DiaryInvalidRequestException("이미지 순서가 중복되었습니다.");
		}
		int userImageCount = 0;
		for (DiaryImageCommand info : infos) {
			if (info.type() == DiaryPhotoType.AI_IMAGE) {
				if (!manageGeneratedImageForDiaryPort.isGeneratedImageOwnedByUser(info.aiPhotoId(), userId)) {
					throw new DiaryInvalidRequestException("유효하지 않은 AI 사진 ID: " + info.aiPhotoId());
				}
			}
			if (info.type() == DiaryPhotoType.USER_IMAGE) {
				if (info.photoIndex() == null) {
					throw new DiaryInvalidRequestException("유효하지 않은 사용자 사진 인덱스: null");
				}
				userImageCount++;
			}
		}
		if (userImageCount != (photos == null ? 0 : photos.size())) {
			throw new DiaryInvalidRequestException("사용자 사진 개수와 이미지 정보 개수가 일치하지 않습니다.");
		}
	}

	private void validatePhotos(List<UploadedFileData> photos) {
		if (photos == null || photos.isEmpty()) {
			return;
		}
		for (UploadedFileData file : photos) {
			String originalFilename = file.originalFilename();

			if (originalFilename == null || !originalFilename.contains(".")) {
				throw new DiaryInvalidRequestException("유효하지 않은 파일 이름입니다: " + originalFilename);
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
				throw new DiaryInvalidRequestException("허용되지 않는 이미지 확장자입니다: " + originalFilename);
			}
		}
	}
}
