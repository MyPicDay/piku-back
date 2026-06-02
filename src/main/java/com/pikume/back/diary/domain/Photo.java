package com.pikume.back.diary.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "photos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Photo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String url;

	private String optimizedUrl;

	@Enumerated(EnumType.STRING)
	private PhotoOptimizationStatus optimizationStatus;

	private LocalDateTime optimizedAt;

	private Integer optimizationAttemptCount;

	private LocalDateTime optimizationLastAttemptAt;

	private Boolean represent;

	private Integer photoOrder;

	@ManyToOne
	@JoinColumn(name = "diary_id")
	@JsonBackReference
	private Diary diary;

	public Photo(Diary diary, String url, Integer photoOrder) {
		this.diary = diary;
		this.url = url;
		this.represent = false;
		this.photoOrder = photoOrder;
		this.optimizationAttemptCount = 0;
		initializeOptimizationStatus(url);
	}

	public void updateRepresent(Boolean represent) {
		this.represent = represent;
	}

	public String getDisplayUrl() {
		if (optimizedUrl != null && !optimizedUrl.isBlank()) {
			return optimizedUrl;
		}
		return url;
	}

	public void markOptimizationSucceeded(String optimizedUrl) {
		this.optimizedUrl = optimizedUrl;
		this.optimizedAt = LocalDateTime.now();
		this.optimizationStatus = PhotoOptimizationStatus.SUCCEEDED;
	}

	private void initializeOptimizationStatus(String url) {
		String extension = extractExtension(url);
		if (extension == null) {
			this.optimizationStatus = PhotoOptimizationStatus.SKIPPED;
			return;
		}

		switch (extension) {
			case "jpg", "jpeg", "png", "bmp" -> this.optimizationStatus = PhotoOptimizationStatus.PENDING;
			case "webp" -> {
				this.optimizedUrl = url;
				this.optimizationStatus = PhotoOptimizationStatus.SUCCEEDED;
				this.optimizedAt = LocalDateTime.now();
			}
			default -> this.optimizationStatus = PhotoOptimizationStatus.SKIPPED;
		}
	}

	private String extractExtension(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		int dotIndex = url.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == url.length() - 1) {
			return null;
		}
		return url.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}
}
