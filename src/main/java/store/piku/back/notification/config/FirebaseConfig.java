package store.piku.back.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;

@Configuration
@Profile("prod")
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.key-path:piku_fcm.json}")
    private String firebaseKeyPath;

    @PostConstruct
    public void initFirebase() {
        try {
            FileInputStream serviceAccount = new FileInputStream(firebaseKeyPath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (Exception e) {
            log.error("Firebase initialization error : {}", e.getMessage());
            throw new RuntimeException("Firebase initialization error", e);
        }
    }

//    @PostConstruct
//    public void init() throws IOException {
//        InputStream serviceAccount;
//    serviceAccount = new FileInputStream(firebaseKeyPath);
//
//        FirebaseOptions options = FirebaseOptions.builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build();
//
//        FirebaseApp.initializeApp(options);
//    }

}