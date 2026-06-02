package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.PhotoOptimizationStatus;

import java.time.LocalDateTime;

public interface SavePhotoOptimizationPort {

	boolean claimPhotoOptimization(Integer photoId, LocalDateTime attemptedAt);

	void markPhotoOptimizationSucceeded(Integer photoId, String optimizedUrl, LocalDateTime optimizedAt);

	void markPhotoOptimizationFailed(Integer photoId, PhotoOptimizationStatus nextStatus, LocalDateTime attemptedAt);

	void markPhotoOptimizationSkipped(Integer photoId, LocalDateTime attemptedAt);
}
