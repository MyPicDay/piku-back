# DDD + 헥사고날 아키텍처 적용 기준

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-25

## 목적

이 문서는 DDD와 헥사고날 아키텍처 원칙을 `piku-back`의 현재 모듈러 모놀리스에 적용하는 프로젝트 규약과 허용 범위를 정의한다.

이 문서는 기존 Aggregate, 데이터 모델, API, 인증 또는 배포 구조를 새로 설계하지 않는다.

## 1. 규칙의 해석 순서

충돌하거나 애매한 상황에서는 다음 순서로 판단한다.

1. 도메인 모델, Ubiquitous Language와 불변식
2. Bounded Context의 모델 소유권과 공개 계약
3. inside/outside 의존 방향과 기술 격리
4. 이 저장소의 패키지·접미사·Annotation 규약
5. 현재 구현의 호환성과 점진적 마이그레이션 제약

하위 순위의 규약을 지키기 위해 상위 순위의 모델 의미를 훼손하지 않는다. 예를 들어 `crosscontext` 패키지로 파일을 옮겼더라도 대상 모델이 소비자에게 그대로 노출되면 경계는 보호되지 않은 것이다.

## 2. 규칙 등급

| 등급 | 의미 | 예시 |
| --- | --- | --- |
| **원칙** | DDD 또는 헥사고날 경계를 보호하기 위해 지켜야 하는 규칙 | 다른 Context의 내부 모델·저장소에 직접 의존하지 않는다. |
| **프로젝트 기본값** | 일관성을 위해 기본으로 따르되 더 나은 근거가 있으면 문서화해 다르게 선택할 수 있는 규약 | In Port는 `UseCase`, Out Port는 `Port` 접미사를 사용한다. |
| **현재 허용 범위** | 프로젝트가 범위를 명시해 허용하는 기술 결합 | Domain Entity의 JPA 매핑 Annotation |

구조를 검토할 때는 세 등급을 구분한다. 원칙 위반과 명명·패키지 기본값 불일치를 같은 심각도로 다루지 않는다.

## 3. 현재 패키지 책임

| 위치 | 현재 책임 | 허용 의존성 | 금지 의존성 |
| --- | --- | --- | --- |
| `domain` | Entity, Value Object, Aggregate, Domain Service, Domain Event와 비즈니스 규칙 | 같은 모델 경계의 Domain 타입, Java 표준 타입, 허용된 JPA 매핑 Annotation | Application, Adapter, 다른 Context의 내부 모델, Spring Data, Web, 외부 SDK |
| `application` | 유스케이스 조정, Application 계약, Port와 트랜잭션 의도 | 같은 모델 경계의 Domain과 Port, Java 표준 타입, 허용된 Spring 구성 Annotation | Adapter 구현, Web DTO, Repository 구현, 공급자 SDK, 다른 Context의 내부 모델 |
| `adapter/in` | HTTP·Scheduler 등의 입력을 Application 호출로 변환 | 같은 Application의 In Port와 Application 계약, 입력 기술 타입 | Out Port 직접 호출, Persistence Adapter, 비즈니스 규칙 소유 |
| `adapter/out` | Out Port를 저장소·외부 시스템·다른 Application으로 구현 | 같은 Application의 Out Port, Domain 타입과 필요한 기술 타입 | Application에 기술 타입 역노출, 다른 Context의 내부 저장 계층 |

패키지는 모델 경계를 구현하는 현재 수단이다. 최상위 패키지가 곧 Bounded Context라는 뜻은 아니며, 경계 상태는 Context Map에서 별도로 관리한다.

### 인증과 보안 기술

- 계정 등록, 이메일 검증, 비밀번호 재설정과 로그인 가능 상태는 해당 계정 Context가 소유한다.
- 로그인, 세션 재발급과 로그아웃은 계정 Context의 Application 유스케이스다.
- 비밀번호 해시, JWT 생성·검증과 갱신 세션 저장은 Out Port 뒤의 Security Adapter가 구현한다.
- Bearer Token·관리자 Cookie를 Spring Security 인증으로 변환하는 Filter는 Security의 입력 Web Adapter다.
- 일반 사용자와 관리자 Principal은 Security가 소유한다. 다른 Context는 입력 Web Adapter에서 인증 식별자를 읽을 때만 Principal을 사용할 수 있고 Domain·Application 계약에는 노출하지 않는다.
- Security는 여러 Context에서 재사용할 수 있는 기술 모듈이며 독립된 비즈니스 Context로 간주하지 않는다. 따라서 자체 Domain과 Application 유스케이스를 소유하지 않는다.
- Security는 User·Admin의 공개 In Port와 공개 DTO만 호출할 수 있다. 두 Context의 Domain, Application Out Port·Service, Repository와 Adapter를 직접 참조하지 않는다.
- 일반 사용자와 관리자는 기술 구현을 재사용할 수 있지만 계정 모델과 인증 정책을 공유하지 않는다.
- 관리자 Filter Chain은 `Order(1)`, 일반 사용자 Filter Chain은 `Order(2)`로 유지한다. Origin, CORS, CSRF, 공개 경로와 Actuator IP 접근은 Security Configuration이 소유한다.

### Global 기술 모듈

- Global은 공통 오류 표현, 페이지 값, 감사 시각 기반과 저장소 설정처럼 여러 경계에서 재사용하는 중립 기술 구성요소만 소유한다.
- Global 타입과 기술 계약은 특정 Context의 Entity, Value Object, Application DTO와 비즈니스 Enum을 참조하거나 노출하지 않는다.
- 여러 Context가 사용한다는 이유만으로 Global 기술 타입을 Shared Kernel의 Domain 모델로 해석하지 않는다.
- 객체 저장소의 바이트 로드·저장·표시 URL 해석처럼 공급자 중립적인 기술 능력은 Global 계약으로 둘 수 있다.
- Object Key, 파일명, 공개 범위, 캐시 정책과 파일 생명주기의 결정은 해당 데이터를 사용하는 Context가 소유한다.
- Security Principal, HTTP·Multipart 변환과 이미지 표시 URL 조합은 입력 또는 출력 Adapter가 담당하며 Application 계약에 노출하지 않는다.
- User는 선택된 캐릭터 식별자만 저장한다. Character가 이미지 참조 형식, 고정 캐릭터 정규화, 이미지 접근 속성과 AI 생성 캐릭터 소유권 검증을 소유하고, User Application은 자신이 소유한 Out Port와 Cross-context Adapter를 통해 사용자·캐릭터 선택 쌍 기반 공개 일괄 조회 계약을 소비한다. User의 조회 모델은 Character 결과를 변경 없이 전달하며 경로를 추론하거나 정규화하지 않는다.
- Global 설정에 필요한 보안 문서 구성값은 Security가 중립 구성 계약으로 제공하며 Global은 Security 구현 설정을 직접 참조하지 않는다.

## 4. 프로젝트 기본값

### APP-PORT-001 · In Port

- `application/port/in`에 두고 인터페이스 이름은 `UseCase`로 끝낸다.
- 외부 행위자가 수행하려는 명령이나 조회 의도를 표현한다.
- 입력과 출력은 Application이 소유한 Command, Query, Result 또는 Read Model을 우선 사용한다.
- Web Request·Response DTO, Cookie, Servlet, Security Principal 같은 입력 기술 타입을 노출하지 않는다.

### APP-PORT-002 · Out Port

- `application/port/out`에 두고 인터페이스 이름은 `Port`로 끝낸다.
- 저장소나 공급자 이름보다 Application이 필요로 하는 목적을 표현한다.
- Spring Data 파생 쿼리, 잠금 방식, 캐시 키와 공급자 이름을 계약에 노출하지 않는다.
- Port 분리는 메서드 수가 아니라 외부 행위자, 변경 이유, 일관성과 대체 가능성을 근거로 한다.

### APP-CONTRACT-001 · 계약 타입

- Port의 정상 결과와 Application이 해석할 수 있는 실패 계약은 Java 기본 타입, 같은 모델 경계의 Domain 타입과 Application 계약을 사용한다.
- Web, JPA, Spring Data, 외부 SDK 타입과 공급자 예외를 메서드 시그니처 또는 Application 오류 모델에 노출하지 않는다. 전역 fallback만 처리할 예상하지 못한 기술 장애를 위해 의미 없는 Context 전용 래퍼를 만드는 것은 요구하지 않는다.
- Persistence 집계 결과는 배열이나 Tuple 대신 의미 있는 Read Model로 변환한다.
- Cross-Context 계약은 소비자와 공급자의 내부 모델 중 하나를 무조건 재사용하지 않고 공개 의미와 번역 필요를 먼저 판단한다.

### APP-XCTX-001 · Cross-Context 통합

현재 모듈러 모놀리스의 기본 동기 통합 방식은 다음과 같다.

1. 소비자 Application이 외부 능력을 자신의 Out Port로 표현한다.
2. 소비자 측 Adapter가 공급자의 공개 In Port 또는 Published Contract를 호출한다.
3. Adapter가 공급자 표현을 소비자 의미로 변환한다.

기본 Adapter 위치는 소비자 모듈의 `adapter/out/crosscontext`이며 `{Target}AdapterFor{Consumer}`처럼 방향을 드러내는 이름을 권장한다.

다만 다음을 명확히 구분한다.

- 소비자 소유 Port와 번역은 모델 보호를 위한 기본값이다.
- `crosscontext` 패키지명과 Adapter 접미사는 저장소 일관성을 위한 규약이다.
- 공급자의 In Port 직접 호출은 같은 프로세스에서 사용할 수 있는 통합 방식이지 유일한 방식은 아니다.
- API, 이벤트, Open Host Service, Published Language, Shared Kernel 또는 Separate Ways가 더 적절하면 별도 설계에서 선택한다.

공급자의 Entity, Out Port, Repository와 Persistence Adapter 직접 사용은 Working Context 경계를 넘는 경우 허용하지 않는다.

## 5. 현재 허용 범위

### Domain Entity의 JPA 매핑

- 현재 모듈러 모놀리스에서는 Domain Entity에 `jakarta.persistence` 매핑 Annotation을 둘 수 있다.
- Spring Data Repository, `EntityManager`, 쿼리와 Persistence Projection까지 허용한다는 뜻은 아니다.
- Domain 상태 변경과 불변식은 Persistence Context 없이 단위 테스트할 수 있어야 한다.

### Application 구성 Annotation

- Application Service의 Spring `Service`, 로컬 트랜잭션 선언을 위한 `Transactional` Annotation은 허용한다.
- Spring Security 구현체, Spring Data 예외, HTTP 타입, 파일 시스템 API, 공급자 SDK와 트랜잭션 동기화 API를 Application 로직이 직접 해석하거나 제어하지 않는다.

### 단일 데이터베이스와 동기 호출

- 같은 애플리케이션과 데이터베이스 안의 동기 호출은 하나의 로컬 트랜잭션에 참여할 수 있다.
- 이는 현재 배포 제약에 따른 선택이며 Context가 같은 데이터 소유권이나 Aggregate 경계를 공유한다는 의미가 아니다.
- 경계 간 원자성이 계속 필요하다면 모델 경계가 올바른지, 공개 계약과 실패 정책이 충분한지 함께 검토한다.

## 6. 오류와 부가 작업

### APP-ERROR-001 · 오류 번역

- 저장 구현은 관계형 데이터베이스, 캐시 저장소와 객체 저장소처럼 상태 또는 파일을 저장하고 조회하는 Outgoing Adapter 구현을 뜻한다. 프로세스 내부·외부 여부나 SDK 사용 여부가 아니라 Port를 구현하는 역할로 판단한다.
- 외부 시스템 연동 구현은 저장 이외의 AI, 푸시, 메일과 외부 API처럼 프로세스 밖의 능력을 SDK, HTTP 또는 다른 프로토콜로 호출하는 Outgoing Adapter 구현을 뜻한다.
- 저장 구현과 외부 시스템 연동에서 발생한 기술 실패 중 Application이 해석하거나 복구할 수 있는 알려진 실패만 Application 결과 또는 오류로 변환한다.
- 연결 장애, 타임아웃과 예상하지 못한 저장소·외부 시스템 오류처럼 Application 의미가 없는 장애는 Application 로직이 공급자 예외를 해석하지 않은 채 전역 기술 오류 처리 경계로 전파할 수 있다.
- 전역 fallback은 처리되지 않은 장애를 위한 마지막 안전망으로 공통 내부 서버 오류 응답과 운영 알림만 담당한다. 예상 가능한 비즈니스 실패를 처리하거나 재시도, 보상과 Domain·Application 정책을 결정하는 제어 흐름으로 사용하지 않는다.
- 다른 Context 실패는 필요할 때 소비자 Context의 결과 또는 오류로 변환한다.
- Web Adapter는 Application 오류를 저장소의 API 오류 응답 표준에 따라 RFC 9457 Problem Details로 변환한다.
- Domain과 Application은 HTTP 상태와 Problem Details 타입을 알지 않는다.

### APP-TX-001 · 커밋 이후 작업

- 알림, 이메일, 외부 파일 정리처럼 핵심 성공 결과를 되돌리지 않아야 하는 작업은 커밋 이후 실행할 수 있다.
- Application Service는 Spring 트랜잭션 동기화 API를 직접 사용하지 않고 실행 의도를 Port로 요청한다.
- best-effort, 재시도, 중복 방지와 전달 보장 수준은 유스케이스 요구에 맞게 명시한다.

## 7. 검증과 적용

- Domain 규칙은 대표 시나리오와 불변식 단위 테스트로 검증한다.
- Web·DB·SDK 없이 Application 유스케이스를 테스트할 수 있어야 한다.
- 아키텍처 테스트는 금지 의존, Port 접미사와 기본 패키지 규약을 검증한다.
- 허용 항목은 적용 범위를 명시하며 신규 코드에서 문서화된 범위를 넘지 않는다.
- 구조 변경이 기능 동작, 데이터 소유권, API 또는 마이그레이션을 바꾸면 별도 설계 없이 진행하지 않는다.
- 관련 없는 Aggregate, 테이블, API와 패키지를 규칙 준수만을 이유로 한 번에 재설계하지 않는다.
