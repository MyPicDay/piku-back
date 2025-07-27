package store.piku.back.auth.constants;

public class AuthConstants {
    public static final String REFRESH_TOKEN = "rn";
    public static final long VERIFICATION_EXPIRATION_MINUTES = 10;
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 1000L * 60 * 30; // 30 minutes
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7; // 7 days
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String DEVICE_ID_HEADER = "Device-Id";
}
