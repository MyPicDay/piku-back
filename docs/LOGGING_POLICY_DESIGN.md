# 로그 정책 설계

## 목표

Piku 백엔드의 애플리케이션 로그를 개인정보 최소화 원칙에 맞게 재설계한다.

이번 설계의 핵심은 세 가지다.

- raw 이메일, 토큰, `Authorization` 헤더, `deviceId`, `client IP`를 기본적으로 로그에 남기지 않는다.
- 로그를 자유 문장형이 아니라 키-값 중심의 이벤트 로그로 통일한다.
- 운영 추적은 `requestId`와 `userId`를 중심으로 수행한다.

이 설계는 “로그를 많이 남기는 것”보다 “운영에 필요한 정보만, 안전하게, 일관되게 남기는 것”을 우선한다.

## 배경

현재 코드에는 이메일이 raw 값으로 직접 로그에 남는 구간이 존재한다.

- [AuthService.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/user/auth/application/service/AuthService.java)
- [TokenService.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/application/service/TokenService.java)
- [JwtProvider.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/jwt/JwtProvider.java)
- [JwtFilter.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/jwt/JwtFilter.java)

또한 JWT 생성/파싱/필터 통과처럼 요청 수에 비례해 매우 자주 발생하는 성공 로그도 `info` 레벨로 남고 있다. 이 방식은 다음 문제를 만든다.

- 개인정보가 로그 수집 시스템, 콘솔, 장애 알림 채널로 불필요하게 확산된다.
- 고빈도 성공 로그가 운영상 중요한 실패 로그를 묻는다.
- 계층마다 같은 실패를 중복 기록해 노이즈가 커진다.
- 로그 포맷이 제각각이라 검색/집계/대시보드 구성이 어렵다.

## 범위

이번 설계는 아래를 포함한다.

- 애플리케이션 로그의 개인정보 정책
- 키-값 중심 로그 포맷
- 로그 레벨 정책
- 중복 로그 방지 원칙
- `requestId`/`userId` 중심 추적 원칙
- 인증, 이메일 인증, JWT, 외부 연동, 예외 처리 로그 가이드

이번 설계는 아래를 포함하지 않는다.

- 감사 로그와 운영 로그를 별도 저장소로 분리하는 체계
- 로그 보관 기간, 접근 권한, 법적 보존 정책
- ELK, Loki, Cloud Logging 같은 외부 로그 플랫폼 선택
- 메트릭, 트레이싱, 알림 시스템 전체 설계

즉 이번 설계는 “애플리케이션이 어떤 로그를 어떤 형식으로 남겨야 하는가”까지만 다룬다.

## 설계 원칙

- 로그는 기본적으로 운영/디버깅 목적의 이벤트 기록이다.
- 개인정보는 기본 비노출이 원칙이다.
- 허용된 식별자만 남기고, 나머지는 남기지 않는다.
- 로그는 키-값 중심으로 통일한다.
- 예측 가능한 비즈니스 실패는 과도하게 `error`로 남기지 않는다.
- 예상 못 한 시스템 실패만 경계에서 강하게 기록한다.
- 같은 실패는 한 요청 경로에서 한 번만 핵심 로그를 남긴다.

## 개인정보 정책

### 기본 금지 대상

아래 값은 raw 형태로 로그에 남기지 않는다.

- `email`
- `phone`
- `token`
- `refreshToken`
- `Authorization` 헤더
- `deviceId`
- `client IP`
- 인증 코드, 검증 코드, 비밀번호

이번 서비스 특성상 운영 중 raw 이메일을 로그에서 직접 확인해야 할 필요가 드물기 때문에, 이메일은 마스킹도 기본 정책에서 제외한다. 즉 “마스킹된 이메일을 남기는 것”이 아니라 “이메일 자체를 남기지 않는 것”이 기본값이다.

### 기본 허용 대상

아래 값은 기본 허용한다.

- `requestId`
- `userId`

단, `userId`는 실제로 확인된 이후에만 남긴다. 비인증 요청, 로그인 실패, 이메일 인증 실패처럼 사용자 식별이 확정되지 않은 구간에서는 `userId` 없이 기록한다.

## 로그 포맷

모든 신규 로그는 키-값 중심으로 작성한다.

권장 기본 필드는 아래와 같다.

- `event`
- `outcome`
- `requestId`
- `userId`
- `reason`
- `resourceId`

필요 시 아래 필드를 추가로 사용할 수 있다.

- `status`
- `problemType`
- `exception`
- `durationMs`

예시:

```text
event=login_failed outcome=denied reason=invalid_credentials requestId=...
event=verification_email_requested outcome=accepted requestId=...
event=profile_image_updated outcome=success userId=... requestId=...
event=unexpected_error outcome=failed requestId=... exception=IllegalStateException
```

자유 문장형 로그는 기존 코드 정리 전까지 일부 남을 수 있지만, 신규 코드와 리팩토링 구간은 위 포맷을 따른다.

## 로그 레벨 정책

### `INFO`

비정상 상태가 아니지만 운영상 의미 있는 상태 변화만 남긴다.

예:

- 로그인 성공
- 로그아웃 성공
- 회원가입 완료
- 이메일 발송 요청 수락
- 프로필 변경 완료

### `WARN`

예상 가능한 실패이지만, 운영상 추적이 필요한 경우에만 남긴다.

예:

- 로그인 실패
- 잘못된 refresh token
- 존재하지 않는 리소스 요청
- 검증 코드 만료

### `ERROR`

예상하지 못한 시스템 오류만 남긴다.

예:

- 외부 연동 실패
- 저장소 읽기 실패
- 예외 핸들러까지 전파된 미처리 예외

### `DEBUG`

고빈도 성공 로그나 내부 처리 흐름은 `DEBUG` 이하로 내리거나 삭제한다.

예:

- JWT 파싱 성공
- JWT 필터 인증 성공
- 토큰 생성 완료

즉 “요청마다 반복되는 성공 로그”는 기본적으로 `INFO`에 두지 않는다.

## 중복 로그 방지

같은 실패를 여러 계층에서 반복 기록하지 않는다.

원칙은 다음과 같다.

- 예상 가능한 비즈니스 실패는 필요한 계층 한 곳에서만 `warn` 또는 무로그 처리한다.
- 예상하지 못한 시스템 예외는 경계 계층에서 한 번만 `error`로 기록한다.
- `GlobalExceptionHandler`나 모듈별 `@RestControllerAdvice`가 최종 실패를 기록한다면, 하위 서비스에서 같은 내용을 다시 `error`로 남기지 않는다.

## requestId / userId 추적 정책

운영 추적의 기본 축은 `requestId`와 `userId`다.

### `requestId`

- 모든 HTTP 요청에 `requestId`를 부여한다.
- 외부에서 들어온 `X-Request-Id`가 있으면 재사용할 수 있다.
- 없으면 서버가 생성한다.
- 로그 MDC에 저장해 모든 하위 로그에서 자동 포함되게 한다.

### `userId`

- 인증이 완료된 이후에만 MDC 또는 명시적 필드로 포함한다.
- 로그인 실패처럼 사용자 식별이 보장되지 않는 구간에서는 `userId`를 남기지 않는다.

이번 설계에서는 `deviceId`, `client IP`를 기본 비노출로 두므로, 운영 상관관계 추적은 `requestId` 중심으로 보는 것을 기본값으로 한다.

## 인증/보안 로그 정책

인증 로그는 중요하지만 개인정보를 노출하면 안 된다.

### 로그인

- 성공: `event=login_succeeded outcome=success userId=... requestId=...`
- 실패: `event=login_failed outcome=denied reason=invalid_credentials requestId=...`

로그인 실패에서는 이메일 존재 여부를 드러내는 메시지를 로그에 남기지 않는다.

### refresh token

- 성공: `event=token_reissued outcome=success userId=... requestId=...`
- 실패: `event=token_reissue_failed outcome=denied reason=invalid_refresh_token requestId=...`

refresh token raw 값은 절대 남기지 않는다.

### JWT 처리

- JWT 생성/파싱/필터 통과 성공 로그는 삭제하거나 `DEBUG`로 내린다.
- 토큰 검증 실패만 필요 시 `WARN`으로 남긴다.

## 이메일/인증 코드 로그 정책

이메일 인증, 비밀번호 재설정, 인증 코드 검증 흐름은 개인정보가 섞이기 쉽다.

원칙:

- 이메일 주소는 raw로 남기지 않는다.
- 인증 코드는 절대 로그에 남기지 않는다.
- “이미 가입된 이메일”, “가입되지 않은 이메일” 같은 정보는 로그에서도 노출을 최소화한다.

권장 예시:

```text
event=verification_email_requested outcome=accepted requestId=...
event=verification_email_request outcome=ignored reason=email_already_registered requestId=...
event=verification_code_failed outcome=denied reason=expired requestId=...
```

## 예외 및 Problem Details 로그 정책

Problem Details 응답과 로그는 서로 역할이 다르다.

- 클라이언트에는 `type`, `status`, `detail`, `instance`를 응답한다.
- 서버 로그에는 필요하면 `problemType`, `status`, `requestId`, `userId`, `exception`을 남긴다.

예상 가능한 예외는 아래처럼 남긴다.

```text
event=request_failed outcome=denied problemType=https://api.pikume.com/problems/auth/invalid-credentials status=401 requestId=...
```

예상하지 못한 예외는 아래처럼 남긴다.

```text
event=request_failed outcome=failed problemType=https://api.pikume.com/problems/common/internal-server-error status=500 requestId=... exception=IllegalStateException
```

## 현재 코드베이스 우선 정리 대상

1. [AuthService.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/user/auth/application/service/AuthService.java)
   - raw 이메일 로그 제거
   - 이메일 인증/비밀번호 재설정 관련 로그를 `event` 중심으로 변경
2. [TokenService.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/application/service/TokenService.java)
   - 로그인/재발급 로그에서 이메일 제거
   - `key=email-deviceId` 같은 로그 제거 또는 대체
3. [JwtProvider.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/jwt/JwtProvider.java)
   - 토큰 생성/파싱 성공 로그 축소 또는 `DEBUG` 하향
4. [JwtFilter.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/security/jwt/JwtFilter.java)
   - 이메일 직접 로그 제거
   - JWT 인증 성공 `INFO` 로그 삭제 또는 `DEBUG` 하향
5. `requestId`를 MDC에 넣는 공통 필터 도입

## 마이그레이션 원칙

- 신규 로그는 즉시 새 정책을 따른다.
- 기존 로그는 민감도와 빈도 기준으로 우선순위를 정해 점진 정리한다.
- 인증, 이메일, JWT 관련 로그를 1차 우선순위로 처리한다.
- 단순 문자열 문장형 로그를 한 번에 전부 없애기보다, 개인정보 노출과 고빈도 노이즈를 먼저 줄인다.

## 완료 기준

아래가 만족되면 이번 로그 정책 작업은 완료로 본다.

- raw 이메일 로그가 인증/이메일 관련 주요 경로에서 제거된다.
- 토큰, 인증 코드, `Authorization`, `deviceId`, `client IP`가 raw로 로그에 남지 않는다.
- 신규 및 정리 대상 로그가 키-값 중심 형식을 따른다.
- `requestId`가 주요 애플리케이션 로그에서 추적 가능하다.
- JWT 성공 고빈도 로그가 `INFO`에서 제거되거나 `DEBUG`로 하향된다.
- 예측 가능한 실패와 시스템 예외가 서로 다른 레벨/형식으로 정리된다.
