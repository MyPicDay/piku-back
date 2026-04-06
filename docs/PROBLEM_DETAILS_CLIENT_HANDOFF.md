# Problem Details Client Handoff

이 문서는 RFC 9457 / Problem Details 전환과 관련해 프론트엔드와 모바일이 반영해야 하는 응답 형식 변경을 정리한다.

이 문서는 클라이언트 계약 기준 문서다. 내부 task 이름이나 구현 순서는 다루지 않는다.

## 공통 규칙

실패 응답은 이제 RFC 9457 `ProblemDetail` 형식을 사용한다.

```json
{
  "type": "https://api.pikume.com/problems/security/invalid-credentials",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "instance": "/api/auth/login"
}
```

클라이언트는 문자열 `message`나 별도 `code`가 아니라 아래 필드를 기준으로 처리한다.

- `type`: 안정적인 문제 유형 식별자
- `status`: HTTP 상태 코드
- `detail`: 화면 표시용 상세 메시지
- `instance`: 실패가 발생한 요청 경로
- `fieldErrors`: validation 실패 시에만 존재

성공 응답 중 단순 문자열을 내려주던 API는 이제 공통 `MessageResponse`를 사용한다.

```json
{
  "message": "회원가입 성공"
}
```

## Validation 실패

요청 값 검증 실패는 공통 validation problem type을 사용한다.

```json
{
  "type": "https://api.pikume.com/problems/validation/invalid-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "요청 값이 올바르지 않습니다.",
  "instance": "/api/diary",
  "fieldErrors": {
    "content": "일기 내용은 비어 있을 수 없습니다."
  }
}
```

클라이언트는 `fieldErrors`가 있을 때 필드별 에러 표시를 한다.

## API별 변경 사항

### 인증

- `POST /api/auth/login`
  - 실패 시 `401 ProblemDetail`
  - `type`
    - `https://api.pikume.com/problems/security/invalid-credentials`
- `POST /api/auth/reissue`
  - 실패 시 `401 ProblemDetail`
  - 성공 시 `MessageResponse`
  - 실패 `type`
    - `https://api.pikume.com/problems/security/invalid-refresh-token`
- `POST /api/auth/logout`
  - 비로그인 시 `401 ProblemDetail`
  - 성공 시 `MessageResponse`
  - 실패 `type`
    - `https://api.pikume.com/problems/security/unauthenticated`

### 회원가입/이메일 인증

- `POST /api/auth/signup`
- `POST /api/auth/send-verification/sign-up`
- `POST /api/auth/send-verification/password-reset`
- `POST /api/auth/verify-code`
- `POST /api/auth/password-reset`

위 API들은 성공 시 모두 `MessageResponse`를 반환한다.

회원가입/이메일 인증 실패는 `AuthProblemType` 기반 Problem Details를 반환한다.

- `https://api.pikume.com/problems/auth/user-not-found`
- `https://api.pikume.com/problems/auth/email-already-exists`
- `https://api.pikume.com/problems/auth/email-verification-required`
- `https://api.pikume.com/problems/auth/code-invalid`
- `https://api.pikume.com/problems/auth/email-send-failure`

### AI 이미지 생성

- `POST /api/diary/ai/generate`
  - 한도 초과 시 `429 ProblemDetail`
    - `https://api.pikume.com/problems/common/rate-limit-exceeded`
  - 생성 실패 시 `500 ProblemDetail`
    - `https://api.pikume.com/problems/common/internal-server-error`

### 알림

- `PATCH /api/sse/{notificationId}`
- `DELETE /api/sse/{notificationId}`

대상 알림이 없으면 `404 ProblemDetail`을 반환한다.

- `type`
  - `https://api.pikume.com/problems/common/resource-not-found`

### 유저

- `PUT /api/users/profile-image`
  - 존재하지 않는 이미지면 `404 ProblemDetail`
  - `type`
    - `https://api.pikume.com/problems/common/resource-not-found`

- `GET /api/users/nickname/availability`
  - 성공 시 기존 `NicknameCheckResponse` 유지
  - 충돌 시 `409 ProblemDetail`
  - `type`
    - `https://api.pikume.com/problems/user/nickname-conflict`

- `PATCH /api/users/profile`
  - 성공 시 기존 `NicknameChangeResponse` 유지
  - 실패 시 `ProblemDetail`
  - `400`
    - `https://api.pikume.com/problems/validation/invalid-request`
  - `404`
    - `https://api.pikume.com/problems/common/resource-not-found`
  - `409`
    - `https://api.pikume.com/problems/user/nickname-conflict`
    - `https://api.pikume.com/problems/user/profile-conflict`

### 캐릭터

- `GET /api/characters/fixed/{fileName}`
  - 파일이 없으면 `404 ProblemDetail`
  - `type`
    - `https://api.pikume.com/problems/common/resource-not-found`

### 일기

- `POST /api/diary`
  - validation 실패: `400 ProblemDetail`
    - `https://api.pikume.com/problems/validation/invalid-request`
  - 저장 실패: `422 ProblemDetail`
    - `https://api.pikume.com/problems/common/unprocessable-content`
  - 서버 오류: `500 ProblemDetail`
    - `https://api.pikume.com/problems/common/internal-server-error`
  - malformed body는 컨트롤러 로컬 처리 대신 전역 `GlobalExceptionHandler`에서 `400 ProblemDetail`로 처리
  - `type`
    - `https://api.pikume.com/problems/common/malformed-request`
- `GET /api/diary/images/{userId}/{filename}`
  - 파일이 없으면 `404 ProblemDetail`
    - `https://api.pikume.com/problems/common/resource-not-found`

### 피드/다이어리 전용 예외

- diary 전용 예외
  - `https://api.pikume.com/problems/diary/not-found`
  - `https://api.pikume.com/problems/diary/forbidden`
  - `https://api.pikume.com/problems/diary/conflict`
- feed 전용 예외
  - `https://api.pikume.com/problems/feed/diary-not-found`

## 클라이언트 반영 포인트

- 실패 분기는 `message`가 아니라 `type`과 `status`를 기준으로 한다.
- 사용자 노출 문구는 `detail`을 사용한다.
- validation 오류는 `fieldErrors`를 우선 사용한다.
- 성공 문자열 응답을 기대하던 곳은 `message` 필드에서 읽는다.

## QA 체크

- 인증 실패 시 문자열 body가 아니라 `ProblemDetail`이 오는지 확인
- validation 실패 시 `fieldErrors`가 내려오는지 확인
- `MessageResponse`로 바뀐 성공 응답에서 `message` 필드가 유지되는지 확인
- `type`이 문서에 적힌 식별자와 일치하는지 확인
