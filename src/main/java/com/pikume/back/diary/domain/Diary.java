package com.pikume.back.diary.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@Table
@NoArgsConstructor
@Getter
public class Diary extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 500)
	private String content;

	@Enumerated(EnumType.STRING)
	private DiaryVisibility status;

	private LocalDate date;

	@Column(name = "user_id", length = 36)
	private String userId;

	public Diary(String content, DiaryVisibility status, LocalDate date, String userId) {
		this.content = content;
		this.status = status;
		this.date = date;
		this.userId = userId;
	}

	public void delete() {
		this.inactive();
	}

	public boolean isOwner(String userId) {
		return this.userId.equals(userId);
	}
}
