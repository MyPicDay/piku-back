package com.pikume.back.recommendation.application.dto;

public record DiaryMetadataResult(Long diaryId, String primaryTopic, String topics, Double qualityScore) {
}
