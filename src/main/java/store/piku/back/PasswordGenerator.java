package store.piku.back;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Scanner;

public class PasswordGenerator {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Spring Security BCrypt 비밀번호 생성기 ===");
        System.out.println("(실제 서버와 100% 호환)");
        System.out.println();
        
        while (true) {
            System.out.print("생성할 비밀번호를 입력하세요 (종료하려면 'quit' 입력): ");
            String password = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(password)) {
                System.out.println("프로그램을 종료합니다.");
                break;
            }
            
            if (password.isEmpty()) {
                System.out.println("비밀번호를 입력해주세요.");
                continue;
            }
            
            // Spring Security BCrypt로 비밀번호 인코딩
            String encodedPassword = encoder.encode(password);
            
            System.out.println();
            System.out.println("=== 결과 ===");
            System.out.println("원본 비밀번호: " + password);
            System.out.println("인코딩된 비밀번호: " + encodedPassword);
            System.out.println("인코딩된 비밀번호 길이: " + encodedPassword.length() + "자");
            System.out.println();
            
            // 검증 테스트
            boolean isValid = encoder.matches(password, encodedPassword);
            System.out.println("검증 테스트: " + (isValid ? "성공 ✓" : "실패 ✗"));
            System.out.println();
            System.out.println("=".repeat(50));
            System.out.println();
        }
        
        scanner.close();
    }
}
