package store.piku.back.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "FCM")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userId;

	private String token;

	private String deviceId;

	public FcmToken(String userId, String token, String deviceId) {
		this.userId = userId;
		this.token = token;
		this.deviceId = deviceId;
	}

	public void updateToken(String newToken) {
		this.token = newToken;
	}
}
