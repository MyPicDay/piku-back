# Google 로그인·회원가입 변경 계약

- Status: 구현 계약, 배포·활성화 전
- Audience: 프론트엔드·모바일 개발자
- Source of Truth: Yes
- Last Reviewed: 2026-09-07

## 클라이언트가 바꿀 흐름

신규 가입은 **인증 → 필수 동의 → 닉네임·캐릭터 설정** 순서다. 인증 화면 제출은 아직 회원 생성이 아니다. 필수 동의를 제출하면 임시 닉네임·기본 캐릭터를 가진 실제 회원과 로그인 세션이 생성된다. 이 회원은 `REQUIRED`이며 프로필 완료 후 `COMPLETED`가 된다.

서버가 반환하는 `progress.nextAction`을 기준으로 진행한다. 로그인했다는 사실만으로 서비스 초기화, 푸시 등록 또는 SSE 연결을 시작하지 않는다. `PROFILE`이면 프로필 설정을 먼저 완료한다. 기존 회원은 보통 `COMPLETE`, 중단한 신규 회원은 같은 userId의 `PROFILE`을 받는다.

| nextAction | 화면의 다음 동작 |
| --- | --- |
| `AUTHENTICATE` | 이메일 또는 Google 인증 |
| `VERIFY_EMAIL` | 소셜 신원은 확인됨. 서비스 이메일을 코드로 인증 |
| `AGREEMENTS` | 최신 필수 약관 표시와 동의 제출 |
| `PROFILE` | 닉네임 점유 후 닉네임·고정 캐릭터 함께 제출 |
| `COMPLETE` | 일반 서비스 진입 |

`progress`에는 `nextAction`, `email`, `userId`, `profileSetupStatus`, `expiresAt`가 있다. 값이 없는 필드는 null일 수 있다. `expiresAt`는 회원 생성 전 가입 인증 증명의 만료 시각이며, 프로필 설정 기한이 아니다.

## API와 인증 전달

### 가입 API 전체 경로

아래 경로는 API 서버 기준 전체 경로다. 웹·모바일은 같은 가입 규칙을 사용하지만 호출 경로와 인증 전달 방식이 다르다.

| 기능 | 메서드 | 웹 엔드포인트 | 모바일 엔드포인트 |
| --- | --- | --- | --- |
| 진행 상태·호출자 초기화 | GET | `/api/auth/signup/progress` | `/api/mobile/auth/signup/progress` |
| 동의 문서 조회 | GET | `/api/auth/signup/agreements` | `/api/mobile/auth/signup/agreements` |
| 이메일 인증 코드 발송 | POST | `/api/auth/signup/email/code` | `/api/mobile/auth/signup/email/code` |
| 이메일 인증 화면 제출 | POST | `/api/auth/signup/email` | `/api/mobile/auth/signup/email` |
| 소셜 가입 이메일 보완 | POST | `/api/auth/signup/social/email` | `/api/mobile/auth/signup/social/email` |
| 동의 제출·회원 생성 | POST | `/api/auth/signup/agreements` | `/api/mobile/auth/signup/agreements` |
| 닉네임 중복 확인·예약 | POST | `/api/auth/signup/nickname` | `/api/mobile/auth/signup/nickname` |
| 닉네임·캐릭터 확정 | POST | `/api/auth/signup/profile` | `/api/mobile/auth/signup/profile` |
| 가입 미완료 회원 탈퇴 | DELETE | `/api/auth/signup/profile` | `/api/mobile/auth/signup/profile` |

### Google 인증 API 전체 경로

Google 인증 API는 위 가입 API의 하위 경로가 아니다. 명시적 기존 계정 연결도 시작 요청의 `link: true`로 같은 엔드포인트를 사용한다.

| 대상·기능 | 메서드 | 엔드포인트 | 호출 방식 |
| --- | --- | --- | --- |
| 웹 Google 인증 시작 | POST | `/api/auth/oauth/google/start` | 프론트가 호출한 뒤 응답의 `authorizationUrl`로 이동 |
| 웹 Google 인증 콜백 | GET | `/api/auth/oauth/google/callback` | Google이 브라우저를 돌려보내는 서버 콜백. `state`와 `code` 또는 `error`를 query로 전달. 프론트가 직접 호출하는 API가 아님 |
| 모바일 Google 인증 준비 | POST | `/api/mobile/auth/oauth/google/challenge` | 앱이 호출해 서버의 `state`·`nonce`를 수신 |
| 모바일 Google 인증 완료 | POST | `/api/mobile/auth/oauth/google/complete` | 앱이 `state`와 Google `idToken`을 제출 |

### 가입 요청·응답

이하 요청·응답 표에서만 하위 경로로 축약한다. 기준 경로는 웹 `/api/auth/signup`, 모바일 `/api/mobile/auth/signup`이다. 본문은 JSON이다. 모바일은 웹 쿠키를 인증 증명으로 사용하지 않는다.

| 메서드·하위 경로 | 요청 | 응답과 의미 |
| --- | --- | --- |
| GET `/progress` | 로그인했다면 Bearer 토큰. 진행 중 증명은 아래 전달 규칙 적용 | `enabled`, `progress`, 웹 `csrfToken` 또는 모바일 `callerBinding`. 페이지 진입으로 DB 가입 레코드를 생성하지 않음 |
| GET `/agreements` | 없음 | 문서 배열: `type`, `version`, `content`, `required` |
| POST `/email/code` | `email`, 선택 `challengeId` | `challengeId`, `expiresAt`, `resendAvailableAt`. 소셜 이메일 인증이면 가입 증명도 전달 |
| POST `/email` | `challengeId`, `email`, `code`, `password` | 이메일 인증·비밀번호 검증 후 `progress`와 가입 증명 전달 |
| POST `/social/email` | `challengeId`, `email`, `code`와 기존 가입 증명 | 같은 소셜 가입 증명의 이메일 인증 완료 |
| POST `/agreements` | `agreements` 배열의 `type`, `version`, `agreed`; 가입 증명과 `Device-Id` 필수 | 회원 생성 후 `progress`, `user`, 로그인 자격 전달 |
| POST `/nickname` | Bearer 토큰, `nickname` | `nickname`, `expiresAt`. 3분 예약 |
| POST `/profile` | Bearer 토큰, `nickname`, `characterId` | `userId`, `nickname`, `characterId`, `profileSetupStatus: COMPLETED` |
| DELETE `/profile` | Bearer 토큰 | 가입 미완료 회원 탈퇴, 204. 완료 회원 탈퇴 API가 아님 |

닉네임은 최대 20자이며 `가입대기_` 접두어를 선택할 수 없다. 같은 닉네임의 중복 확인은 기존 예약 시간을 연장하지 않는다. 예약이 만료되면 중복 확인을 다시 수행한다. 새 닉네임 예약 실패 시 이전 예약은 유지된다. 캐릭터는 기존 GET `/api/characters/fixed`의 실제 ID를 사용한다. 기본 캐릭터를 그대로 선택해도 완료할 수 있다.

## 웹

GET `/progress`로 받은 `csrfToken`을 이후 신규 가입 쓰기와 Google 시작 요청의 `X-Signup-CSRF` 헤더로 전송한다. 허용된 `Origin`과 쿠키가 함께 필요하다. 쿠키는 host-only, Secure, HttpOnly, SameSite=Lax다. 브라우저 요청은 쿠키를 포함하도록 구성한다. 개발 환경도 이 쿠키 계약에 맞는 HTTPS를 준비한다.

웹 가입 증명은 `__Host-pk-signup-proof` 쿠키로 전달되며 응답 본문에 노출하지 않는다. 호출자 결합·CSRF 쿠키도 서버가 관리한다. 증명·세션 토큰을 URL, 로컬 저장소 또는 로그에 복사하지 않는다.

회원 생성 또는 기존 회원 로그인 성공 시 액세스 토큰은 `Authorization: Bearer …` 응답 헤더, 갱신 토큰은 기존 `rn` HttpOnly 쿠키로 전달한다. `user`는 기존 `id`, `nickname`, `avatarUrl`에 `profileSetupStatus`가 추가된다. 일반 로그인과 GET `/api/auth/me`도 이 상태를 반환한다.

현재 동의 완료 응답에는 임시 닉네임과 기본 캐릭터의 이미지 URL이 포함되며 `characterId`는 포함되지 않는다. 캐릭터 ID를 응답만으로 기본 선택값에 연결하려면 후속 계약 보완이 필요하다. 이미지 URL을 파싱해 캐릭터 ID를 추론하지 않는다.

Google 로그인 버튼에서는 POST `/api/auth/oauth/google/start`에 `link: false`와 `Device-Id`를 보낸다. 응답 `authorizationUrl`로 이동한다. 서버 콜백은 설정된 프론트 복귀 주소로 303 이동하며 URL에 가입 증명·서비스 토큰을 넣지 않는다.

복귀 시 이전 액세스 토큰을 재사용하지 말고 서버의 새 결과를 확인한다. 기존 회원이면 `rn`으로 POST `/api/auth/reissue` 후 GET `/api/auth/signup/progress`를 조회한다. 신규 가입이면 갱신 쿠키가 비워지고 가입 증명 쿠키가 있으므로, 토큰 없는 GET `/progress`에서 이메일 보완 또는 동의를 이어간다. 재발급의 401만으로 가입 증명까지 삭제하지 않는다.

명시적 연결은 기존 계정 로그인 상태에서 `link: true`, 현재 비밀번호를 시작 요청에 함께 제출한다. 대상 회원은 Bearer 토큰으로 결정한다. 클라이언트가 targetUserId나 임의 복귀 주소를 보내지 않는다. 기존 회원 연결에는 새 약관 동의가 없다.

## 모바일

GET `/progress`에서 받은 `callerBinding`을 가입 진행 동안 보관하고 이후 `X-Signup-Binding` 헤더로 보낸다. 인증 성공 응답의 `proof`는 `X-Signup-Proof` 헤더로 전달한다. 웹 CSRF 헤더와 쿠키는 사용하지 않는다. 기기·토큰 보관은 모바일의 기존 인증 저장 방식에 맞추되 URL이나 로그에 넣지 않는다.

POST `/api/mobile/auth/oauth/google/challenge`에 서버에 등록된 `registration`, `link: false`, `Device-Id`, 호출자 헤더를 보낸다. 응답의 `state`, `nonce`, `expiresAt` 중 nonce를 해당 Google 인증 요청에 결합한다. 얻은 ID 토큰과 state를 POST `/api/mobile/auth/oauth/google/complete`의 `idToken`, `state`로 제출한다. 등록 이름은 백엔드가 안내한 값을 사용하고 audience/clientId를 대신 보내지 않는다.

회원이 결정된 응답은 `tokens`에 기존 모바일 형식인 `tokenType`, `accessToken`, `refreshToken`, `accessTokenExpiresIn`, `refreshTokenExpiresIn`을 담는다. 새 회원 인증 단계에서는 `tokens` 없이 `proof`와 `progress`를 받는다. 모바일 현재 회원 조회는 GET `/api/mobile/auth/me`다. 기존 로그인·재발급·로그아웃 API는 유지한다.

명시적 연결은 인증된 Bearer 토큰과 `link: true`, 기존 비밀번호를 challenge 요청에 추가한다. 앱은 개발 중이므로 새 계약을 적용하며 구형 모바일 가입 계약을 장기 유지하지 않는다.

## 실패와 재시도

JSON API 오류는 RFC 9457 `application/problem+json`이며 `type`, `title`, `status`, `detail`, `instance`를 확인한다. 신규 가입의 비즈니스 오류에는 `code`가 추가된다. 요청 형식·필드 검증 오류는 기존 공통 검증 응답을 사용하므로 `code`가 없을 수 있다. 회원 기능의 접근 제한은 `type`이 `/problems/signup/profile-setup-required`인 403으로 구분한다.

브라우저 이동용 Google 콜백에서 사용자가 취소하거나 인증·가입 조건이 거부되면, 동일한 프론트 복귀 주소에 서버가 정의한 `oauthError` 코드만 붙여 303 복귀한다. 예를 들어 `GOOGLE_CANCELLED`, `OAUTH_EXPIRED`, `OAUTH_REPLAY`, `ACCOUNT_LINK_CONFLICT`를 처리한다. 제공자의 원문 오류나 서비스 자격은 URL에 넣지 않는다. 이 경우 새 세션이 생겼다고 간주하지 말고 해당 안내 후 로그인을 다시 시작한다. 이 브라우저 복귀 계약과 JSON API의 Problem Details를 구분한다.

| 오류·상황 | 처리 |
| --- | --- |
| `PROOF_EXPIRED` 410 | 회원 생성 전 인증을 다시 시작 |
| `AGREEMENT_VERSION_MISMATCH` 409 | 최신 문서를 다시 표시하고 동의 받기 |
| `EMAIL_REQUIRED` 400 | 같은 소셜 가입 증명으로 이메일 보완 |
| `ACCOUNT_LINK_CONFLICT`, `EMAIL_ALREADY_REGISTERED` 409 | 기존 로그인 사용. 임의 계정 병합이나 다른 이메일 덮어쓰기 금지 |
| `HOLD_REQUIRED` 409 | 닉네임 중복 확인을 다시 수행 |
| `NICKNAME_UNAVAILABLE` 409 | 다른 닉네임 선택. 이전 예약은 유지 |
| `PROFILE_ALREADY_COMPLETED` 409 | 현재 상태 조회 후 일반 서비스 또는 일반 프로필 변경 흐름 |
| `RATE_LIMITED`, `ATTEMPTS_EXHAUSTED`, `OAUTH_RATE_LIMITED` 429 | 잠시 후 재요청. 발송 응답의 재발송 가능 시각 준수 |
| `OAUTH_EXPIRED` 410, `OAUTH_REPLAY` 409, Google 인증 실패 | 이전 state/코드/토큰을 재사용하지 않고 Google 로그인 재시작 |
| `CSRF_INVALID`, `CALLER_REQUIRED` | 진행 조회로 쿠키·호출자 초기화 후 요청 갱신 |
| `SIGNUP_DISABLED` 503 | 신규 가입 일시 중지 안내. 기존 회원 로그인 유지 여부와 구분 |

동의 응답 유실 시 동일한 증명과 같은 동의 내용으로 재시도할 수 있다. 프로필 완료 응답 유실 시 같은 닉네임·캐릭터로 재시도할 수 있다. 회원 생성 후에는 증명 만료와 관계없이 다시 로그인해 프로필을 마칠 수 있다.

## 기존 웹 전환

전환 기간의 POST `/api/auth/signup`은 실제 이메일 인증 성공 시 발급한 가입 증명 쿠키와 같은 브라우저 결합을 요구한다. 이메일 문자열만으로 인증 사실을 가져오지 않는다. 기존 성공 본문은 유지하며 허용 Origin의 JSON 요청과 쿠키 포함이 필요하다. 신규 단계별 가입이 활성화되면 구형 가입·가입 코드 발송·가입 목적 코드 확인은 410 `LEGACY_SIGNUP_DISABLED`로 닫힌다. 비밀번호 재설정 흐름은 별도다.

Google 전용 신규 회원은 비밀번호가 없으며 비밀번호 재설정으로 비밀번호를 추가할 수 없다. Google 로그인을 안내한다. `+tag`와 점을 제거해 이메일을 합치지 않는다.

이 문서는 백엔드가 제공하는 화면 흐름과 API 계약만 다룬다. 프론트·모바일의 파일 구성, 상태 관리, SDK 선택과 구현 순서는 각 저장소에서 결정한다.
