package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;

import java.util.List;

public interface LoadPhotoOptimizationPort {

	List<PhotoOptimizationTarget> findPendingPhotoOptimizationTargets(int limit);
}
