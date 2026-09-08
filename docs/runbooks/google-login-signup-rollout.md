# Google 로그인·회원가입 전환 운영

- Status: 활성화 전 운영 기준
- Audience: 백엔드·운영 담당자
- Source of Truth: Yes
- Last Reviewed: 2026-09-08

## 배포와 활성화 구분

코드와 스키마 배포만으로 Google 로그인이나 신규 가입을 활성화하지 않는다. `signup.enabled`와 `signup.google.enabled`의 기본값은 false다. 실제 약관·Google 등록·암호화 키·복귀 주소·기본 캐릭터 준비를 확인한 후 기능을 연다. 이 문서가 운영 배포 승인을 의미하지 않는다.

## 필요한 설정

| 설정 | 기본값 또는 요구 조건 |
| --- | --- |
| `signup.enabled` | false. 새 인증·동의 가입 허용 여부 |
| `signup.legacy-signup-enabled` | true. 단, 신규 가입이 활성화되면 자동으로 구형 가입을 차단 |
| `signup.legacy-email-accounts-verified` | false. 기존 비밀번호 회원의 Gmail 자동 연결을 허용하기 전 실제 가입 경로·이관 계정 출처 확인 |
| `signup.agreements` | 실제 문서별 type, version, content, required. 활성화 시 유일한 종류·비어 있지 않은 내용·버전·필수 문서 존재 검증 |
| `signup.max-code-attempts` | 5 |
| `signup.resend-seconds` | 60. 새 challengeId를 만들어도 같은 이메일의 재발송 제한 적용 |
| `signup.email-hourly-limit` / `origin-hourly-limit` | 5 / 30 |
| `signup.cleanup-interval-ms` | 3,600,000 |
| `signup.google.enabled` | false. 이미 연결된 Google 회원 로그인까지 포함하는 인증 기능 스위치 |
| `signup.google.web-client-id` / `web-client-secret` / `web-redirect-uri` | 실제 Google 웹 등록과 정확히 일치. 콜백은 `/api/auth/oauth/google/callback` |
| `signup.google.mobile-registrations` | 서버 등록 이름별 audience, authorizedParty. 앱이 제출하는 이름을 고정하고 실제 Google 토큰 발급 구조에 맞춰 설정 |
| `signup.protocol.encryption-key` | Google 활성화 시 필요한 Base64 인코딩 32바이트 키. 관리형 비밀 저장소에서 주입 |
| `signup.protocol.caller-hourly-limit` / `origin-hourly-limit` | 20 / 100 |
| `signup.protocol.cleanup-batch-size` / `cleanup-interval-ms` | 500 / 300,000 |
| `signup.web.completion-uri` | Google 인증 후 돌아갈 신뢰하는 프론트 HTTPS 주소. query·fragment·userinfo 없는 고정 주소 |

실제 비밀 키·클라이언트 secret·토큰을 문서나 로그에 넣지 않는다. 모든 인스턴스가 동일한 암호화 키를 사용해야 한다. 이 키는 DB의 OAuth nonce·PKCE verifier를 암호화하므로 무계획한 교체는 진행 중 로그인을 복구 불가능하게 만든다. 교체 시 새 인증 시작을 잠시 닫고 기존 10분 요청 만료를 기다린 후 모든 인스턴스를 같은 키로 전환한다.

모바일 등록의 audience와 authorizedParty를 서로 같은 값이라고 가정하지 않는다. 앱에 제공할 것은 서버 등록 이름이며 검증 허용 목록을 사용자 입력으로 확장하지 않는다. Google 외의 제공자는 이 릴리스에서 활성화할 수 없다.

## 전환 순서

1. DB 백업과 기존 이메일 가입·수동 생성·이관 계정의 출처를 확인한다. 검증 이력이 확실하지 않으면 자동 연결 설정을 켜지 않고 명시적 기존 로그인 연결을 사용한다.
2. `base_image_1.webp`를 해석하는 유일한 고정 Character와 표시용 자산이 실제 카탈로그에 있는지 확인한다. 임의 숫자 ID나 가짜 기본 캐릭터를 주입하지 않는다.
3. V16–V19를 적용한다. V16은 기존 회원을 `COMPLETED`로 유지하며 탈퇴 정보와 기존 이메일·닉네임·캐릭터를 변경하지 않는다. 이후 마이그레이션은 가입 증명, 연결, 동의, 검증 challenge, DB 예약과 OAuth 요청·제한 저장소를 추가한다.
4. 모든 백엔드 인스턴스를 새 닉네임 DB 잠금과 접근 제어를 사용하는 버전으로 교체한다. 기존 메모리 예약을 사용하는 구버전 인스턴스와 새 예약 기능을 동시에 운영하지 않는다. 필요한 전환 동안 가입·닉네임 쓰기를 잠시 중지한다.
5. 구형 웹이 Origin의 JSON 요청에 쿠키를 포함하고 실제 이메일 검증 후 발급된 증명 쿠키를 유지하는지 확인한다. 전환 중 `/api/auth/signup`은 이 증명과 호출자 결합을 요구한다.
6. 웹·모바일에서 새 API와 인증 전달 규칙에 맞춰 상태 분기, 신규 동의, 3분 예약, OAuth 복귀·nonce 결합을 적용한다. 다른 저장소의 구현을 이 백엔드 배포에 섞지 않는다.
7. 약관과 Google·암호화·복귀 설정을 적용하고 스테이징에서 신규 이메일 가입, 기존 Gmail 연결, 신규 Google 가입, 중단 후 재개, 회원 제한, 구형 가입 차단을 확인한다. 모바일 실제 ID 토큰의 audience·azp·nonce도 확인한다.
8. 준비된 클라이언트에 신규 가입을 활성화한다. `signup.enabled=true`에서는 구형 가입·가입 코드 발송·가입 목적 확인이 닫힌다. 같은 시점에 구형 경로로 동의를 우회하지 못하는지 확인한다.

가입 증명과 OAuth 요청은 인증 성공/시작 기준 10분에 만료된다. cleanup 주기는 만료 시각을 늘리지 않는다. `REQUIRED` 회원 자체는 만료 정리 대상이 아니다.

가입 만료 정리는 별도 `READ_COMMITTED` 트랜잭션으로 실행한다. MySQL에서 binary log를 사용하면 `binlog_format`이 `ROW` 또는 `MIXED`인지 확인한다. `STATEMENT` 설정은 이 격리 수준의 쓰기와 호환되지 않는다. 일반 가입 쓰기의 격리 수준을 전역 변경하지 않는다.

웹 증명 쿠키는 DB 증명의 실제 남은 시간으로 설정한다. 만료·무효·구 가입 증명 또는 탈퇴·삭제 회원의 잔존 증명이 남은 진행 조회는 200 인증 시작 상태로 복구하고 해당 쿠키를 삭제한다. 세션 유실로 회원 생성 이후 진행을 재개할 수 없으면 갱신 후 재조회하거나 기존 로그인으로 같은 회원을 복구한다. 새 가입 시도로 회원을 다시 만들지 않는다.

클라이언트 전환 검증에는 만료 후 재인증, 다른 이메일로 인증 재시작, 동의 후 새로고침과 기본 캐릭터 그대로 완료, 로그인·로그아웃 후 이전 가입 증명 정리를 포함한다. 캐릭터는 동의·로그인·본인 조회의 실제 ID와 고정 목록을 비교한다.

## 장애와 중지

| 상황 | 대응 |
| --- | --- |
| 신규 가입만 중지 | `signup.enabled=false`와 `signup.legacy-signup-enabled=false`를 함께 적용. 구형 동의 우회 경로를 다시 열지 않음 |
| 신규 가입 중지 중 Google 기존 회원 | `signup.google.enabled=true`이면 기존 subject 연결 로그인을 유지. 연결이 없는 새 가입은 거절 |
| Google 검증·코드 교환 장애 | 요청은 실패 또는 재사용 불가 상태. 새 Google 로그인을 안내하고 기존 이메일 로그인 유지 |
| OAuth 연결 저장 후 완료 기록·응답 장애 | 연결은 이미 커밋되었을 수 있음. `FAILED`·`PROCESSING`·`CONSUMED`만으로 연결 취소를 추정하지 않고 새 state·nonce로 같은 Google 계정을 재인증. 기존 state 재사용·연결 삭제 금지 |
| DB 장애 | 가입·동의·예약 쓰기를 실패로 처리. Redis나 메모리로 임시 우회하지 않음 |
| 메일 장애 | 성공한 인증으로 표시하지 않음. 재발송 제한을 유지하고 클라이언트가 새 발송을 요청 |
| 기본 캐릭터 누락 | 회원·동의·연결 생성 전체 롤백. 카탈로그 복구 후 같은 유효 증명으로 재시도 |
| 닉네임 충돌·예약 만료 | 409 응답과 중복 확인 재요청. 예약 해제만 별도 커밋하지 않음 |
| OAuth 키 불일치 | 안전하게 인증 실패 처리. 인스턴스별 키 주입 일치 여부를 확인하고 새 요청으로 재시작 |

롤백은 기능 진입을 닫고 현재 스키마·데이터·프로필 접근 제어를 유지하는 방식이 우선이다. 새 상태를 모르는 구버전 바이너리로 즉시 돌아가면 미완료 회원이 회원 기능에 접근하거나 닉네임 예약을 우회할 수 있다. 새로 연결된 Google 전용 회원의 로그인 수단도 고려해야 한다. 테이블 삭제나 회원 일괄 삭제를 롤백 절차로 사용하지 않는다.

## 관찰과 검증

Google 검증 실패, 신규 가입 실패, 코드 재시도 한도, 기본 캐릭터 해석 실패, 닉네임 충돌, DB 잠금 대기와 `REQUIRED` 회원 수를 관찰한다. 닉네임 쓰기와 새 요청 제한은 DB 잠금 행을 이용하므로 실제 트래픽에서 대기 시간을 확인한다. 테스트의 동시 실행 성공을 운영 처리량 보장으로 해석하지 않는다.

토큰·인증 코드·OAuth code·state·쿠키·비밀번호·이메일을 요청 로그에 남기지 않도록 프록시와 애플리케이션 로그 정책도 확인한다. OAuth 콜백 query와 모바일 ID 토큰 본문을 원문으로 수집하지 않는다.

검증은 일반 Gradle 테스트와 MySQL migration 태그 테스트를 구분한다. H2 테스트만으로 MySQL 닉네임 collation, subject 비교, 마이그레이션 호환성을 입증하지 않는다. 운영 Google 계정과 앱의 실제 인증은 설정 주입 후 별도 확인한다.

실제 MySQL 8.4와 적용 마이그레이션을 사용해 서로 다른 가입 증명의 이메일·subject 고유 충돌 복구, 코드 확인·재발송 양방향 경쟁, 만료 정리·가입 쓰기 경쟁, OAuth 연결 커밋 이후 완료 기록의 장애를 검증한다. 잠금 대기는 DB에서 관측하고 순서를 고정한다. 메일 발송·Google 코드 교환과 토큰 검증은 테스트 대역이므로 실제 제공자 연동 성공을 뜻하지 않는다.

활성화 전 환경 검증에서는 다음 결과를 별도로 남긴다.

- 실제 Google 웹 등록의 전체 redirect URI, 코드 교환과 서명 키 조회가 성공하는지 확인한다.
- 모바일 등록 이름과 실제 토큰의 audience·authorized party 대응, 서버 nonce가 반영된 새 ID 토큰 발급, 앱 복귀와 재시작 후 상태 복구를 확인한다.
- 실제 웹·API 도메인에서 HTTPS 쿠키의 Secure·HttpOnly·SameSite, 허용 Origin·CORS, 303 복귀 후 세션 재발급·CSRF 흐름을 확인한다.
- 모든 인스턴스의 OAuth 암호화 키 주입 일치, 적용 스키마와 DB 격리·binary log 조건, 실제 메일 전달과 갱신 세션 저장을 확인한다. 비밀값 자체는 기록하지 않는다.
- 실제 필수 문서의 본문·버전, 기본 캐릭터 ID·자산, 기존 이메일 계정의 인증 가입 출처를 확인한 후 신규 가입·Google 활성화 여부를 결정한다.
