package store.piku.back.notification.application.port.in;

public interface FcmTokenUseCase {

	void saveToken(String userId, String token, String deviceId);
}
