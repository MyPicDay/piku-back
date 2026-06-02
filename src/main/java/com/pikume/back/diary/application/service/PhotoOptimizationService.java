package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.application.port.out.LoadPhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.SavePhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.WebpImageConversionPort;
import com.pikume.back.diary.domain.PhotoOptimizationStatus;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.LoadObjectPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoOptimizationService {

	private static final String WEBP_CONTENT_TYPE = "image/webp";
	private static final String PUBLIC_PREFIX = "public/";
	private static final String PUBLIC_CACHE_CONTROL = "public, max-age=31536000, immutable";

	private final LoadPhotoOptimizationPort loadPhotoOptimizationPort;
	private final SavePhotoOptimizationPort savePhotoOptimizationPort;
	private final LoadObjectPort loadObjectPort;
	private final StoreObjectPort storeObjectPort;
	private final WebpImageConversionPort webpImageConversionPort;
	private final PhotoOptimizationProperties properties;

	public int optimizePendingPhotos() {
		List<PhotoOptimizationTarget> targets = loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(
				properties.getBatchSize());
		int optimizedCount = 0;

		for (PhotoOptimizationTarget target : targets) {
			if (optimizeTarget(target)) {
				optimizedCount++;
			}
		}

		return optimizedCount;
	}

	private boolean optimizeTarget(PhotoOptimizationTarget target) {
		LocalDateTime attemptedAt = LocalDateTime.now();
		if (!savePhotoOptimizationPort.claimPhotoOptimization(target.photoId(), attemptedAt)) {
			log.debug("event=photo_optimization_claim outcome=skipped photoId={} diaryId={}",
					target.photoId(),
					target.diaryId());
			return false;
		}

		try {
			String optimizedKey = PhotoWebpObjectKey.fromOriginal(target.originalUrl()).orElse(null);
			if (optimizedKey == null) {
				savePhotoOptimizationPort.markPhotoOptimizationSkipped(target.photoId(), LocalDateTime.now());
				log.info("event=photo_optimization outcome=skipped photoId={} diaryId={} objectKey={}",
						target.photoId(),
						target.diaryId(),
						target.originalUrl());
				return false;
			}
			byte[] originalBytes = loadObjectPort.loadObject(target.originalUrl());
			byte[] webpBytes = webpImageConversionPort.convertToWebp(originalBytes, properties.getQuality());
			String cacheControl = optimizedKey.startsWith(PUBLIC_PREFIX) ? PUBLIC_CACHE_CONTROL : null;

			storeObjectPort.storeObject(
					new UploadedFileData(fileName(optimizedKey), WEBP_CONTENT_TYPE, webpBytes),
					optimizedKey,
					cacheControl);
			savePhotoOptimizationPort.markPhotoOptimizationSucceeded(target.photoId(), optimizedKey, LocalDateTime.now());
			log.info("event=photo_optimization outcome=succeeded photoId={} diaryId={} objectKey={} optimizedKey={}",
					target.photoId(),
					target.diaryId(),
					target.originalUrl(),
					optimizedKey);
			return true;
		} catch (Exception e) {
			PhotoOptimizationStatus nextStatus = nextFailureStatus(target);
			savePhotoOptimizationPort.markPhotoOptimizationFailed(target.photoId(), nextStatus, LocalDateTime.now());
			log.warn("event=photo_optimization outcome=failed photoId={} diaryId={} status={} attemptCount={} objectKey={} reason={}",
					target.photoId(),
					target.diaryId(),
					nextStatus,
					target.attemptCount() + 1,
					target.originalUrl(),
					e.getMessage());
			return false;
		}
	}

	private PhotoOptimizationStatus nextFailureStatus(PhotoOptimizationTarget target) {
		int nextAttemptCount = target.attemptCount() + 1;
		if (nextAttemptCount >= properties.getMaxAttempts()) {
			return PhotoOptimizationStatus.FAILED;
		}
		return PhotoOptimizationStatus.PENDING;
	}

	private String fileName(String objectKey) {
		int slashIndex = objectKey.lastIndexOf('/');
		if (slashIndex < 0 || slashIndex == objectKey.length() - 1) {
			return objectKey;
		}
		return objectKey.substring(slashIndex + 1);
	}
}
