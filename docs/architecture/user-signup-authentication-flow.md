# 일반 회원 인증과 단계별 가입 흐름

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-09-07

## 책임과 경계

User Context의 `user.auth` Application은 이메일·Google 인증 결과, 가입 증명, 동의 후 회원 생성과 세션 발행을 조정한다. 별도 인증 Context를 새로 만들지 않는다. `User`가 프로필 설정과 탈퇴 상태를 보호하며 닉네임·캐릭터 완료는 User 프로필 유스케이스가 처리한다.

Security는 비밀번호 해시·JWT·갱신 세션의 기술 구현과 HTTP 인증·인가를 담당한다. 공개 `QueryUserAccessUseCase`로 현재 회원 존재·탈퇴·프로필 상태를 조회하며 User Domain이나 Persistence에 직접 접근하지 않는다. 토큰의 과거 상태만으로 완료 여부를 허용하지 않는다.

Character는 가입 기본값과 선택 가능한 고정 캐릭터를 해석한다. User는 자신이 소유한 Out Port와 Adapter를 거쳐 Character의 공개 In Port를 호출하고 실제 캐릭터 식별자만 저장한다. 파일명·저장소·카탈로그 유형 정책은 Character에 둔다.

## 화면별 입력과 가입 상태

웹 `/api/auth/signup`과 모바일 `/api/mobile/auth/signup`의 입력 Adapter는 같은 `SignupFlowUseCase`를 사용한다. 인증, 동의, 프로필 설정 화면의 제출마다 서버 상태를 전이한다. 페이지 진입은 호출자 결속 자격을 준비할 뿐 가입 레코드·이벤트를 생성하지 않는다.

웹은 Secure·HttpOnly·host-only 쿠키와 허용 Origin·CSRF 검증으로 증명과 호출자를 결속한다. 모바일은 전용 호출자·증명 헤더와 응답 본문을 사용한다. Application은 Servlet, Cookie, 제공자 SDK와 응답 상태 코드를 알지 않는다.

인증 성공 결과는 고정 10분짜리 DB 증명이다. 이메일 보완이 필요하면 같은 증명에서 인증한다. 이메일과 필수 동의가 준비되면 실제 User를 임시 닉네임·기본 캐릭터·`REQUIRED` 상태로 생성한다. 이후 진행은 User 상태를 기준으로 하며 증명 만료 때문에 회원을 삭제하거나 프로필 설정 기한을 제한하지 않는다.

`ReserveSignupNicknameUseCase`는 3분 DB 예약을 만들고 `CompleteSignupProfileUseCase`는 닉네임·캐릭터·완료 상태·예약 해제를 함께 확정한다. 회원 조회 계약은 `REQUIRED` 회원을 공개 프로필·참조·요약에서 제외하되 본인 인증과 가입 재개를 유지한다.

## 외부 Google 인증

웹 시작은 state·nonce·PKCE verifier와 브라우저·기기·목적·연결 대상 회원을 DB 요청에 묶는다. 웹 콜백은 짧은 트랜잭션에서 요청을 선점한 뒤 Google 코드 교환을 수행한다. 모바일은 서버가 발행한 state·nonce와 등록 이름에 ID 토큰을 결합한다.

Google Out Adapter는 공식 SDK의 서명·발급자·시간 검증에 더해 서버 등록에 대한 audience·authorized party와 nonce를 확인한다. Application은 검증된 `GoogleIdentity`만 소비하며 클라이언트가 제시한 이메일이나 subject를 신원으로 신뢰하지 않는다. nonce·PKCE 암호화와 요청 제한은 각각 기술 Out Adapter와 DB 저장소에 둔다.

기존 `(provider, subject)`가 있으면 같은 User로 로그인한다. 신규 subject의 Gmail 자동 연결, 기존 로그인 후 명시적 연결, 이메일 보완과 신규 증명 생성은 User가 소유한다. 상세 정책은 [제품 계약](../product-specs/google-login-signup.md)을 따른다.

웹 콜백은 성공·예상 가능한 실패 모두 서버 설정의 고정 HTTPS 프론트 주소로 복귀한다. 실패는 제한된 서버 오류 코드만 query에 전달하며 토큰·증명·원문 제공자 오류를 넣지 않는다. 일반 JSON API 오류는 공용 `ProblemDetailFactory`를 통한 RFC 9457 응답으로 번역한다.

## 트랜잭션과 복구

| 작업 | 원자성·재시도 경계 |
| --- | --- |
| 이메일 코드 검증 | 호출자·목적·시도·유효기간을 검증하고 실패 횟수를 커밋. 유효 코드 검증 후에만 가입 비밀번호 해시 계산 |
| 동의 후 회원 생성 | User·동의·소셜 연결·증명 소비를 한 로컬 DB 트랜잭션으로 저장. 같은 증명·제출 지문은 같은 userId로 복구 |
| 세션 발행 | 회원 생성 커밋 이후 공통 `IssueUserSessionUseCase`로 발행. 실패 시 유효한 동일 증명의 재시도 또는 기존 로그인으로 생성된 계정 복구 |
| 비밀번호 재설정 | 최초 회원 조회에서 행 잠금을 획득하고 최신 탈퇴·비밀번호 상태를 확인. 가입 완료·탈퇴와 겹쳐도 과거 닉네임·프로필 상태·탈퇴 정보를 덮어쓰지 않음 |
| OAuth 요청 | `PENDING` → `PROCESSING` 선점을 먼저 커밋하고 외부 통신 수행. 성공 `CONSUMED`, 실패 `FAILED`; 중복·불명확한 처리 결과는 새 인증으로 재시작 |
| 닉네임·프로필 | 사용자 선택 닉네임은 공유 DB 잠금 후 회원 잠금. 최종 저장과 예약 해제를 함께 커밋하여 실패 시 기존 예약 유지 |

가입 증명·OAuth 요청·회원·예약의 원본은 DB다. Redis를 필수 가입 상태 저장소로 두지 않으며 DB 장애에서 메모리로 우회하지 않는다. 외부 Google 통신을 DB 트랜잭션 안에 유지하지 않고 외부 시스템까지 exactly-once를 보장한다고 가정하지 않는다. 만료 저장·비교는 UTC를 사용한다.

프로필 변경·완료·가입 중 탈퇴는 닉네임 잠금 다음에 회원 행을 잠근다. 비밀번호 재설정은 회원 행만 잠그고 닉네임 잠금을 추가 요청하지 않아 잠금 순서가 역전되지 않는다.

## 호환과 출시 경계

V16–V19는 기존 회원을 `COMPLETED`로 보존하고 새 가입 자료 구조를 확장한다. 기존 동의나 Google 연결은 일괄 생성하지 않는다. 구 가입은 전환 기간에만 호출자 결속 증명으로 보호하며 신규 가입을 켜면 종료한다.

신규 가입과 Google 프로토콜 스위치는 독립이다. 신규 가입 중지 중에도 Google이 활성화되어 있으면 기존 subject 연결 로그인은 유지한다. `REQUIRED`가 생성된 뒤에는 상태 제한을 모르는 과거 바이너리로 롤백하지 않는다.

공개 필드·오류·화면 복구 계약은 [프론트·모바일 전달 문서](../handoffs/google-login-signup-handoff.md), 실제 설정과 전환 순서는 [운영 문서](../runbooks/google-login-signup-rollout.md)를 따른다. 다른 저장소의 구현은 이 백엔드 변경에 포함하지 않는다.
