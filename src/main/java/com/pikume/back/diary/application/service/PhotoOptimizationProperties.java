package com.pikume.back.diary.application.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "photo.optimization")
public class PhotoOptimizationProperties {

	private boolean enabled = false;
	private int batchSize = 10;
	private long fixedDelayMs = 300_000L;
	private int maxAttempts = 3;
	private float quality = 0.82f;
}
