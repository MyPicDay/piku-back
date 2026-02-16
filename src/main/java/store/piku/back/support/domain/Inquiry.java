package store.piku.back.support.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@Column(nullable = false, length = 1000)
	private String content;

	private String imageUrl;

	public Inquiry(String userId, String content, String imageUrl) {
		this.userId = userId;
		this.content = content;
		this.imageUrl = imageUrl;
	}
}
