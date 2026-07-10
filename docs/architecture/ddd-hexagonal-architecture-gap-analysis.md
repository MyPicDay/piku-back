# DDD + 헥사고날 아키텍처 미비점 분석

- Status: Draft
- Audience: Engineers
- Source of Truth: No
- Assessed At: 2026-07-10
- Baseline Commit: d058f16

## 목적

현재 구현을 `docs/architecture/ddd-hexagonal-architecture.md`의 의존 규칙과 `docs/architecture/bounded-context-map.md`에 기록된 현재 책임에 대조하여 구조적으로 미비한 지점을 기록한다.

이 문서는 구현 방안이나 작업 순서를 정의하지 않는다. 소스 코드를 인용하지 않고 문제 위치, 판정 사유, 구조적 영향만 기록한다.

상세 발견 사항의 파일과 라인은 별도 현재 상태 설명이 없는 한 Baseline Commit 당시 근거다. 기준 문서가 보강된 뒤에도 과거 발견 위치를 보존하고, 해결 상태와 검증 근거는 별도로 추가한다.

## 판정 기준

| 기준 규칙 | 판정 기준 |
| --- | --- |
| `HEX-LAYER-001`, `HEX-FLOW-001` | Incoming Adapter는 In Port만 호출하고 Application은 Domain과 자신이 소유한 Out Port에 의존해야 한다. |
| `HEX-LAYER-002`, `HEX-DOMAIN-001` | Application과 Domain은 각 규칙에서 허용한 Annotation 외의 Web, DB, 보안 구현, 외부 SDK 세부사항을 직접 해석하지 않아야 한다. |
| `HEX-PORT-IN-001`, `HEX-PORT-OUT-001` | In Port는 `UseCase`, Out Port는 `Port` 접미사를 사용하고 하나의 명확한 호출 목적과 변경 이유를 표현해야 한다. |
| `HEX-PORT-OUT-002`, `HEX-PERSIST-001` | Port와 Application은 Web·JPA·SDK 타입, 공급자 예외, 배열·Tuple 기반 Persistence Projection을 노출하지 않아야 한다. |
| `HEX-XCTX-001`, `HEX-XCTX-002`, `HEX-XCTX-003` | 호출 Context가 Out Port와 Cross-Context Adapter를 소유하고 Adapter는 대상의 공개 In Port와 공개 계약만 호출해야 한다. |
| `HEX-ERROR-001` | 기술·대상 Context 오류는 호출 Context의 의미로 번역하고 Web Adapter가 RFC 9457 Problem Details로 변환해야 한다. |
| `HEX-TX-001`, `HEX-TX-002` | 로컬 트랜잭션과 커밋 이후 부가 작업을 구분하고 Application이 Spring 동기화 API를 직접 제어하지 않아야 한다. |
| `HEX-TEST-001` | 의존 규칙과 기존 예외 축소를 아키텍처 테스트로 검증해야 한다. |

## 진행 현황

체크되지 않은 항목은 미해결 상태다. 모든 근거 위치가 해소되고 관련 테스트와 문서 검증이 완료된 뒤에만 체크한다. 일부 위치만 개선된 경우에는 체크하지 않고 상세 항목에 부분 진행 내용을 기록한다.

- [ ] **ARCH-01 · 높음**: Diary Application Service가 Cross-Context Out Port를 우회한다. (`HEX-XCTX-001`)
- [ ] **ARCH-02 · 높음**: Cross-Context Adapter가 대상 도메인의 Out Port, Entity, Repository에 접근한다. (`HEX-XCTX-002`)
- [ ] **ARCH-03 · 높음**: Incoming Adapter가 In Port를 우회하거나 Out Port 방향을 역전시킨다. (`HEX-FLOW-001`)
- [ ] **ARCH-04 · 높음**: Application 계층에 외부 SDK, 보안, DB, HTTP 기술이 노출된다. (`HEX-LAYER-002`)
- [ ] **ARCH-05 · 높음**: Cross-Context 계약이 대상 도메인의 모델을 호출 도메인에 노출한다. (`HEX-PORT-OUT-002`, `HEX-XCTX-002`)
- [ ] **ARCH-06 · 중간**: HTTP 요청 정보와 Web Request DTO가 Application In Port 계약에 포함된다. (`HEX-PORT-IN-001`)
- [ ] **ARCH-07 · 중간**: Application Service가 트랜잭션 완료 시점을 Spring API로 직접 제어한다. (`HEX-TX-002`)
- [ ] **ARCH-08 · 중간**: Port 계약이 변경 이유가 다른 능력을 결합하거나 저장 기술을 노출한다. (`HEX-PORT-OUT-001`)
- [ ] **ARCH-09 · 중간**: Persistence 조회 결과 형태가 Application 계층까지 노출된다. (`HEX-PERSIST-001`)
- [ ] **ARCH-10 · 낮음**: Cross-Context Adapter가 표준 패키지 구조를 따르지 않는다. (`HEX-XCTX-001`)
- [x] **ARCH-11 · 낮음**: 기준 문서의 Context 목록과 책임 설명이 부정확하다. 2026-07-10 문서 보강으로 해결했다. (`HEX-CTX-001`)
- [ ] **ARCH-12 · 낮음**: 아키텍처 의존 규칙의 자동 검증 범위가 일부 도메인에 한정되어 있다. (`HEX-TEST-001`)

## 상세 발견 사항

### ARCH-01: Diary Application Service가 Cross-Context Out Port를 우회한다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:30-31` | Diary Application Service가 recommendation과 social의 In Port를 직접 import한다. 호출 도메인의 Out Port와 Cross-Context Adapter를 거치지 않는다. |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:55-56` | 타 도메인 In Port가 Application Service의 직접 의존성으로 고정되어 있다. 대상 도메인 계약 변경이 Diary Service에 바로 전파된다. |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:289` | 친구 목록 조회가 Diary 소유의 Cross-Context Out Port를 거치지 않는다. 같은 도메인에 이미 존재하는 친구 관계 Out Port 사용 방식과도 일관되지 않는다. |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:317` | 일기 메타데이터 분석이 Diary 소유의 외부 능력으로 추상화되지 않고 recommendation 유스케이스에 직접 결합된다. |

영향: Diary Application 계층이 조정자 역할과 Cross-Context 변환 역할을 동시에 가지며, 단위 테스트와 대상 도메인 교체 범위가 넓어진다.

### ARCH-02: Cross-Context Adapter가 대상 도메인의 내부 경계에 접근한다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/admin/adapter/out/crosscontext/AdminStatisticsSourceAdapter.java:6-7,18-19` | admin 어댑터가 diary와 user의 Out Port를 직접 주입받는다. 대상 도메인의 공개 In Port를 사용해야 한다. |
| `src/main/java/com/pikume/back/diary/adapter/out/crosscontext/UserAdapterForDiary.java:6-8,14-32` | user의 Application 예외, Out Port, Entity를 직접 사용한다. user의 조회 방식과 Entity 구조가 Diary 어댑터에 노출된다. |
| `src/main/java/com/pikume/back/notification/adapter/out/crosscontext/UserAdapterForNotification.java:8-10,16-30` | user의 Application 예외, Out Port, Entity를 직접 사용한다. Notification 관점의 사용자 정보 계약으로 변환되기 전에 대상 내부 모델에 결합된다. |
| `src/main/java/com/pikume/back/social/adapter/out/crosscontext/UserAdapterForSocial.java:6,18,21-23` | social 어댑터가 user Out Port를 직접 호출한다. 대상 도메인의 공개 In Port가 우회된다. |
| `src/main/java/com/pikume/back/support/adapter/out/crosscontext/UserAdapterForSupport.java:6,12,15-16` | support 어댑터가 user Out Port를 직접 호출한다. 사용자 존재 확인이라는 호출 목적이 대상 저장 포트에 결합된다. |
| `src/main/java/com/pikume/back/feed/adapter/out/user/UserAdapterForFeed.java:5,14,18-24` | feed의 user 어댑터가 user 공개 In Port가 아니라 diary가 소유한 Out Port를 재사용한다. Feed가 Diary의 외부 의존 계약에 종속된다. |
| `src/main/java/com/pikume/back/user/auth/adapter/out/identity/UserAdapterForAuth.java:6-7,15,23-29` | auth 어댑터가 상위 user 도메인의 Entity와 JPA Repository에 직접 접근한다. Cross-Context 경계와 Persistence 경계를 동시에 우회한다. |

영향: 호출 도메인이 대상 도메인의 저장 구조와 도메인 모델 변경을 흡수하지 못하고 함께 변경되어야 한다.

ARCH-02를 개선할 때에는 대상 Context가 `application/port/in`으로 제공하는 In Port와 Application 계약을 사용해야 한다. 별도의 공개 계약 패키지 신설은 이번 문서의 필수 조건이 아니다.

### ARCH-03: Incoming Adapter가 In Port를 우회하거나 Port 방향을 역전시킨다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/user/auth/adapter/in/web/AuthController.java:16,34,85-95` | Web Adapter가 이메일 발송 Out Port를 직접 호출한다. 이메일 허용 정책과 조회 기능은 In Port 유스케이스로 공개되어야 한다. |
| `src/main/java/com/pikume/back/admin/adapter/in/web/problem/AdminExceptionHandler.java:6,24,48-51` | Web 예외 처리기가 Application Out Port인 telemetry Port를 직접 호출한다. Incoming Adapter가 Out Port의 호출 주체가 되어 의존 방향이 역전된다. |
| `src/main/java/com/pikume/back/diary/adapter/in/scheduler/PhotoOptimizationScheduler.java:3,14,18` | Scheduler Adapter가 In Port가 아닌 구체 Application Service에 의존한다. 서비스 구현 교체와 진입 계약 분리가 불가능하다. |
| `src/main/java/com/pikume/back/notification/application/port/in/SseUseCase.java:3,7` | In Port가 Out Port의 연결 추상화를 입력으로 받는다. 진입 계약과 출력 계약이 서로 분리되지 않는다. |
| `src/main/java/com/pikume/back/notification/adapter/in/web/SseEmitterConnection.java:6,11` | Web Incoming Adapter의 객체가 Application Out Port를 구현한다. 웹 연결 객체가 유스케이스 입력과 출력 양쪽 경계 역할을 동시에 가진다. |
| `src/main/java/com/pikume/back/notification/adapter/in/web/NotificationController.java:50-55` | Controller가 웹 연결 구현체를 생성해 유스케이스에 전달한다. Application 계층이 연결 생명주기를 관리하게 된다. |

영향: In Port가 유스케이스의 의도를 표현하지 못하고 특정 입출력 기술과 생명주기에 종속된다.

### ARCH-04: Application 계층에 외부 기술 세부사항이 노출된다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/notification/application/port/out/PushNotificationPort.java:3,9` | Out Port가 Firebase SDK 예외를 계약에 노출한다. 외부 공급자 교체와 실패 변환 책임이 Adapter 내부에 머물지 않는다. |
| `src/main/java/com/pikume/back/security/application/service/TokenService.java:5,18,37-38` | Application Service가 Spring Security 암호화 구현과 JWT 구현체에 직접 의존한다. 토큰 발급과 비밀번호 검증이 Out Port로 분리되지 않았다. |
| `src/main/java/com/pikume/back/user/auth/application/service/AuthService.java:5,37` | auth Application Service가 Spring Security 암호화 구현에 직접 의존한다. 비밀번호 보호 능력이 Application Port로 추상화되지 않았다. |
| `src/main/java/com/pikume/back/social/application/service/CommentService.java:5,181-186` | Application Service가 Spring DB 접근 예외를 직접 해석한다. Persistence Adapter가 저장 실패를 Application 의미로 변환하지 않는다. |
| `src/main/java/com/pikume/back/social/application/service/LikeService.java:5,56-60` | Application Service가 Spring 무결성 예외를 직접 처리한다. 데이터베이스 제약 표현이 Application 계층까지 누출된다. |
| `src/main/java/com/pikume/back/admin/application/exception/AdminProblem.java:4,9-24,39` | Application 예외 정의가 HTTP 상태 타입과 직접 결합된다. 다른 도메인의 Problem Type이 Web Adapter에 있는 구조와도 일관되지 않는다. |
| `src/main/java/com/pikume/back/diary/application/service/PhotoOptimizationProperties.java:5,9` | Spring 설정 바인딩 객체가 Application Service 패키지에 있다. 운영 설정 Adapter와 유스케이스 정책의 경계가 분리되지 않는다. |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:27,57,461` | Application Service가 웹과 로컬 파일 시스템 책임이 혼재된 공용 유틸리티에 직접 의존한다. 파일 형식 판단 능력이 Port로 표현되지 않는다. |
| `src/main/java/com/pikume/back/global/util/FileUtil.java:5,7-11,61-180,242-254` | 공용 유틸리티가 character 도메인 타입, Spring Web 파일 타입, 로컬 파일 시스템 책임을 함께 가진다. `global`이 도메인 중립적인 공통 계층으로 유지되지 않는다. |

영향: Application 계층의 테스트가 기술 프레임워크를 알아야 하며, 외부 공급자와 저장 기술 변경의 영향이 Adapter 밖으로 확산된다.

### ARCH-05: Cross-Context 계약이 대상 도메인의 모델을 노출한다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/feed/application/port/out/LoadRecommendationForFeedPort.java:3,16` | feed 소유 Out Port가 recommendation 전용 결과 타입을 반환한다. 출력은 feed가 필요로 하는 의미의 타입이어야 한다. |
| `src/main/java/com/pikume/back/feed/application/service/FeedCompositionService.java:7,31-61` | Feed Application Service가 recommendation 결과 타입을 직접 사용한다. 대상 도메인의 계약 변경이 feed 구성 로직에 전파된다. |
| `src/main/java/com/pikume/back/user/auth/application/port/out/LoadUserForSignUpPort.java:3,11-13` | auth 소유 Out Port가 user Entity를 입출력으로 사용한다. 호출 도메인 계약이 대상 도메인 Entity 구조를 노출한다. |
| `src/main/java/com/pikume/back/user/auth/application/service/AuthService.java:21,59-64,125-138` | auth Application Service가 user Entity를 생성하고 변경한다. 사용자 생성과 비밀번호 변경의 소유권이 user와 auth 사이에서 분리되지 않는다. |

영향: bounded context 간 Anti-Corruption Layer가 형식적으로만 존재하고, 실제 모델 소유권은 공유된다.

### ARCH-06: HTTP 요청 정보와 Web Request DTO가 Application In Port에 포함된다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/global/dto/RequestMetaInfo.java:3-10` | scheme, host, 전체 URL, User-Agent, client IP 등 HTTP 요청 정보를 하나의 공용 DTO로 정의한다. |
| `src/main/java/com/pikume/back/diary/application/port/in/CreateDiaryUseCase.java:5,12-13` | Diary In Port가 HTTP 메타데이터를 유스케이스 입력으로 받는다. |
| `src/main/java/com/pikume/back/diary/application/port/in/GetCalendarUseCase.java:5,10-11` | Calendar In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/feed/application/port/in/GetFeedUseCase.java:6,10-12` | Feed In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/notification/application/port/in/NotificationUseCase.java:3,11-15` | Notification In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/social/application/port/in/CommentUseCase.java:3,16-27` | Comment In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/social/application/port/in/FriendUseCase.java:3,19-25` | Friend In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/social/application/port/in/LikeUseCase.java:3,12` | Like In Port가 실제 반환값 생성에 필요하지 않은 HTTP 메타데이터를 입력으로 받는다. |
| `src/main/java/com/pikume/back/user/application/port/in/GetUserProfileUseCase.java:3,15-20` | User Profile In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/user/application/port/in/SearchUserUseCase.java:3,16` | User Search In Port가 HTTP 메타데이터에 의존한다. |
| `src/main/java/com/pikume/back/global/util/ImagePathToUrlConverter.java:25-42,56-75` | URL 변환 과정은 전달된 HTTP 메타데이터를 사용하지 않으므로 다수 In Port의 HTTP 의존이 불필요하게 유지된다. |
| `src/main/java/com/pikume/back/security/application/port/in/LoginUseCase.java:3-5,9-13` | Login In Port가 Web Request DTO와 쿠키 속성 계약을 직접 노출한다. 쿠키 생성 책임은 Web Adapter에 있어야 한다. |
| `src/main/java/com/pikume/back/user/auth/application/port/in/SignUpUseCase.java:3,7` | Sign-up In Port가 Swagger와 Bean Validation이 포함된 Web Request DTO를 직접 입력으로 받는다. |
| `src/main/java/com/pikume/back/user/auth/application/port/in/ResetPasswordUseCase.java:3,7` | Password Reset In Port가 Web Request DTO를 직접 입력으로 받는다. |
| `src/main/java/com/pikume/back/user/auth/application/port/in/VerifyEmailUseCase.java:3,11` | Email Verification In Port가 Web Request DTO를 직접 입력으로 받는다. |

영향: HTTP가 아닌 다른 Incoming Adapter를 추가하기 어렵고, API 스키마 변경이 Application 계약 변경으로 이어진다.

### ARCH-07: Application Service가 트랜잭션 완료 시점을 직접 제어한다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/diary/application/service/DiaryCommandService.java:7-8,187-204,302-317` | 이미지 정리와 메타데이터 분석 실행 시점을 Spring 트랜잭션 동기화 API로 직접 제어한다. Application Service가 트랜잭션 인프라와 후처리 전달 책임을 함께 가진다. |
| `src/main/java/com/pikume/back/notification/application/service/NotificationService.java:7-8,148-159` | 알림 전송 시점을 Spring 트랜잭션 동기화 API로 직접 제어한다. 실패 재처리와 전달 보장 정책이 Port 또는 이벤트 경계로 분리되지 않는다. |

영향: 트랜잭션이 없는 실행 경로와 있는 실행 경로의 동작이 달라지고, 후처리 실패의 재시도와 관찰 가능성을 일관되게 보장하기 어렵다.

### ARCH-08: Port 계약이 변경 이유가 다른 능력을 결합하거나 저장 기술을 노출한다

`load`, `save`, `delete` 용어 자체는 위반이 아니다. Aggregate 저장·조회 목적이 명확하고 책임이 좁은 Persistence Out Port에서는 사용할 수 있다. 아래 항목은 이름이 아니라 계약 크기, Spring Data식 조회 표현과 기술 세부사항 노출을 기준으로 판정한다.

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/diary/application/port/out/LoadDiaryPort.java:31-74` | 단건 조회, 갤러리, 통계, 사진, 피드 후보 조회처럼 변경 이유가 다른 능력을 하나의 Port에 결합한다. |
| `src/main/java/com/pikume/back/user/application/port/out/LoadUserPort.java:22-39` | 사용자 식별, 다건 사용자 조회, 활성·누적 통계를 하나의 Port에 결합해 변경 이유가 다른 조회 책임을 함께 가진다. |
| `src/main/java/com/pikume/back/social/application/port/out/LoadLikePort.java:11-23` | 단건 조회, 잠금 조회, 존재 확인, 집계와 다건 사용자 상태 조회를 결합하고 `ForUpdate` 잠금 방식과 배열 집계 결과를 노출한다. |
| `src/main/java/com/pikume/back/user/auth/application/port/out/LoadVerifiedEmailPort.java:10` | `findTopByEmailAndTypeOrderByVerifiedAtDesc`가 Spring Data 파생 쿼리 정렬 방식을 Port 계약에 노출한다. |
| `src/main/java/com/pikume/back/admin/application/port/out/LoadAdminAccountPort.java:11-23` | 일반 조회, 잠금 조회, 존재 확인과 역할·상태 집계를 한 Port에 결합하고 `ForUpdate` 잠금 방식을 노출한다. |
| `src/main/java/com/pikume/back/user/auth/application/port/out/LoadUserForSignUpPort.java:9-13` | 등록 여부 조회, Entity 조회와 저장을 하나의 Port에 결합하고 User Entity를 Auth 계약에 노출한다. ARCH-05의 모델 노출 문제도 함께 가진다. |

영향: 필요한 능력만 의존하기 어렵고 조회·잠금·집계 구현 변경이 서로 무관한 Application Service와 테스트에 전파된다.

### ARCH-09: Persistence 조회 결과 형태가 Application 계층까지 노출된다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/social/application/port/out/LoadCommentPort.java:16` | 집계 조회 결과를 의미 있는 Application Read Model이 아닌 배열 형태로 반환한다. 컬럼 순서와 타입 정보가 암묵적이다. |
| `src/main/java/com/pikume/back/social/application/service/CommentService.java:165-168` | Application Service가 배열 인덱스와 숫자 타입 변환을 직접 해석한다. Persistence projection 구조가 Application 로직에 노출된다. |
| `src/main/java/com/pikume/back/social/application/port/out/LoadLikePort.java:21` | 집계 조회 결과를 의미 있는 Application Read Model이 아닌 배열 형태로 반환한다. |
| `src/main/java/com/pikume/back/social/application/service/LikeService.java:138-142` | Application Service가 배열 인덱스와 구체 타입을 직접 해석한다. 조회 구현 변경을 Adapter가 흡수하지 못한다. |

영향: 조회 컬럼 순서나 타입이 바뀌면 컴파일 시점에 감지하지 못하고 Application Service가 런타임 오류를 일으킬 수 있다.

### ARCH-10: Cross-Context Adapter가 표준 패키지 구조를 따르지 않는다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/main/java/com/pikume/back/diary/adapter/out/friend/FriendAdapterForDiary.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/feed/adapter/out/diary/DiaryAdapterForFeed.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/feed/adapter/out/recommendation/RecommendationAdapterForFeed.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/feed/adapter/out/social/SocialAdapterForFeed.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/feed/adapter/out/user/UserAdapterForFeed.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/user/adapter/out/character/CharacterAdapterForUser.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/user/adapter/out/diary/DiaryAdapterForUser.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/user/adapter/out/friend/FriendAdapterForUser.java:1` | 대상 도메인명 기반 패키지이며 기준 문서상 `crosscontext` 이전 대상이다. |
| `src/main/java/com/pikume/back/user/auth/adapter/out/character/CharacterAdapterForAuth.java:1` | 대상 도메인명 기반 패키지이며 auth 하위 도메인의 Cross-Context Adapter 위치가 표준과 다르다. |
| `src/main/java/com/pikume/back/user/auth/adapter/out/identity/UserAdapterForAuth.java:1` | 대상 도메인 의미 기반 패키지이며 Cross-Context 표준 위치가 아니다. Repository 직접 접근 문제도 함께 가진다. |

영향: 패키지 이름만으로 외부 기술 Adapter와 타 도메인 Adapter를 일관되게 구분할 수 없다.

기준 문서의 표준 위치는 호출 Context의 `adapter/out/crosscontext`다. 이 항목은 기존 `adapter/out/{대상Context}` 위치를 변경 범위 안에서 점진적으로 정리하기 위한 것이며, 대상별 하위 패키지 신설을 요구하지 않는다.

### ARCH-11: 기준 문서의 Context 목록과 책임 설명이 부정확하다

| 파일 및 라인 | 사유 |
| --- | --- |
| `docs/architecture/ddd-hexagonal-architecture.md:12-31` | 실제 최상위 패키지에 존재하는 admin이 도메인 목록에 없고, 현재 존재하지 않는 ai 패키지가 인프라 항목에 남아 있다. Source of Truth 문서와 코드 탐색 결과가 일치하지 않는다. |
| `docs/architecture/ddd-hexagonal-architecture.md:17-18,30` | user.auth를 인증·인가 하위 도메인으로, security를 인증 인프라로 설명하지만 실제로는 가입·이메일 인증·비밀번호 변경과 로그인·토큰 발급의 소유권이 두 경계에 걸쳐 있다. |
| `src/main/java/com/pikume/back/user/auth/application/service/AuthService.java:29-38` | user.auth가 가입, 이메일 인증, 비밀번호 변경을 소유한다는 실제 책임을 보여 주지만 기준 문서에는 이 구분이 없다. |
| `src/main/java/com/pikume/back/security/application/service/TokenService.java:30-38` | security가 로그인, 토큰 발급, 로그아웃을 소유한다는 실제 책임을 보여 주지만 기준 문서에는 user.auth와의 경계가 정의되지 않는다. |

영향: 신규 기능이 어느 bounded context에 속하는지 판단하기 어렵고, auth와 security 사이의 중복 의존이 계속 추가될 수 있다.

해결 상태: 2026-07-10에 `ddd-hexagonal-architecture.md`를 공통 의존 규칙 중심으로 보강하고, `bounded-context-map.md`에 현재 Context와 모듈의 실제 책임을 정리했다. `user.auth`와 `security`의 현재 역할도 기록하되 새로운 모델이나 책임 재편을 확정하지 않았다. Baseline Commit 당시의 문서 불일치 근거는 보존한다.

### ARCH-12: 아키텍처 의존 규칙의 자동 검증 범위가 제한적이다

| 파일 및 라인 | 사유 |
| --- | --- |
| `src/test/java/com/pikume/back/architecture/ArchitectureBoundaryTest.java:17-77` | security와 creative의 일부 의존 규칙은 자동 검증하지만, Diary Application Service의 타 도메인 In Port 직접 의존과 Diary의 대상 도메인명 Adapter 패키지를 차단하는 규칙은 없다. |

영향: 이미 검증 대상으로 등록된 일부 경계는 보호되지만, ARCH-01과 같은 Diary Cross-Context 우회가 다시 추가되어도 CI에서 차단하지 못한다.

## 제외 및 참고 사항

- Domain Entity의 `jakarta.persistence` 매핑 Annotation은 `HEX-DOMAIN-001`이 허용하므로 미비점에서 제외했다. Spring Data Repository, EntityManager, 쿼리와 Persistence Projection 의존은 허용 범위에 포함되지 않는다.
- Spring의 Service 및 Transactional Annotation 자체는 `HEX-LAYER-002`가 허용하므로 제외했다. 다만 구체 SDK, DB 예외, 보안 구현체, 트랜잭션 동기화 API처럼 Application 로직이 직접 해석하거나 제어하는 의존성은 포함했다.
- API 오류 응답은 확인된 주요 Web Adapter와 예외 처리기가 공용 Problem Details 생성 경로를 사용하므로 별도 미비점으로 분류하지 않았다.
