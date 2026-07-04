# DDD + 헥사고날 아키텍처 문서

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-04

이 문서는 `piku-back` 프로젝트의 도메인 주도 설계(DDD) 및 헥사고날 아키텍처 구조를 설명합니다.

## 1. 도메인 종류

프로젝트 내 `com.pikume.back` 하위에는 다음과 같은 패키지들이 존재합니다.

### 비즈니스 도메인

- **diary**: 다이어리 작성, 조회, 캘린더 등 핵심 기록 기능
- **user**: 사용자 계정, 프로필 관리
  - `auth`: 인증/인가를 담당하는 하위 도메인 (자체 adapter/application/domain 구조 보유)
- **social**: 친구 관계, 차단 등 사용자 간 소셜 네트워크 기능
- **feed**: 공개 다이어리 피드 목록 제공
- **notification**: 푸시 알림(FCM), SSE 실시간 알림 등 알림 발송 기능
- **recommendation**: 피드나 친구 추천 알고리즘 및 데이터 제공
- **creative**: 스티커, 테마 등 꾸미기 및 창작 요소
- **character**: 캐릭터 관리 기능 (헥사고날 구조)
- **support**: 고객 지원, 문의 등 서포트 기능 (헥사고날 구조)

### 인프라/공통

- **ai**: AI 연동 서비스 (단순 `service/` 구조, 비헥사고날)
- **security**: 인증 필터, JWT, OAuth 등 보안 인프라 (헥사고날 + `config/`, `dto/`, `jwt/` 혼재)
- **global**: 공통 예외 처리, 설정, 유틸리티(파일 업로드/다운로드 포함) 등 인프라성 기능

## 2. 각 도메인의 의미와 역할

- **독립적인 비즈니스 단위**: 각 도메인은 내부적으로 비즈니스 규칙과 엔티티를 캡슐화하며, 책임을 분명히 분리합니다.
  - 예: `diary`는 사용자의 일기 데이터 생성과 달력 조회만 집중하며, 친구 관계는 `social` 도메인이 전담합니다.

## 3. 폴더 구조 (헥사고날 아키텍처)

각 도메인은 대체로 헥사고날 아키텍처 (포트 앤 어댑터) 패턴에 따라 아래와 같이 구성됩니다.

- **domain**: 비즈니스 로직의 핵심. JPA Entity, VO(Value Object) 등 핵심 모델이 위치합니다.
- **application**: 비즈니스 유스케이스 구현체.
  - `port/in`: 클라이언트(웹 등)가 서비스를 호출하기 위한 인터페이스 (UseCase).
  - `port/out`: 서비스가 외부(DB, 타 도메인, 외부 API)와 통신하기 위한 인터페이스 (Port).
  - `service`: `in` 포트를 구현하고 도메인 객체를 활용하여 비즈니스로직을 수행하는 곳. DB나 외부 연결은 `out` 포트를 통해 진행합니다.
- **adapter**: 애플리케이션 외부와의 실제 연결 고리 구현체.
  - `in`: 주로 `web` 계층의 REST Controller들이 존재하여, 외부 HTTP 요청을 Application 계층의 UseCase로 변환하여 넘깁니다.
  - `out`: DB 접근(`persistence` - Repository 등), 파일 스토리지(`storage`), 외부 시스템 연동 등을 수행하며 Application 계층의 `out` 포트를 구현합니다.

> **참고**: 일부 도메인에는 `config/`(notification, security) 등 헥사고날 3계층 외에 추가 폴더가 도메인 루트 레벨에 존재합니다.

## 4. 같은 도메인 내에서 사용하는 방법

1. **흐름**: `Controller (Adapter In)` -> `UseCase (Port In)` -> `Service (Application)` -> `Domain Entity` 활용 -> `Port (Port Out)` -> `Repository (Adapter Out)`
2. 서비스 객체(`application/service`)는 구체적인 DB 기술(JPA 등)을 모른 채 `port/out` 인터페이스에만 의존하여 데이터를 조작합니다.
3. 데이터 조작은 `domain` 패키지에 위치한 엔티티(`Entity`)를 통해 이뤄집니다.

## 5. Port 메서드 작성 원칙

Port 메서드는 저장소 구현 방식이 아니라 Application 계층이 필요로 하는 유스케이스의 의도를 표현해야 합니다. Repository 관용 메서드명이 상위 계층으로 전파되면 저장소 구조나 조회 기준이 바뀔 때 Application Service, UseCase, 타 도메인 연동 코드까지 함께 흔들릴 수 있습니다.

- `application/port/in`의 메서드는 외부 요청이 수행하려는 유스케이스를 드러내는 이름을 사용합니다.
- `application/port/out`의 메서드는 Application Service가 필요로 하는 도메인 정보나 외부 능력을 드러내는 이름을 사용합니다.
- `findById`, `save`, `delete`처럼 저장소 구현에 가까운 이름은 실제 Repository나 Persistence Adapter 내부에 머무르게 합니다. 식별자 기반 조회가 필요하더라도 Port 경계에서는 `loadUserForDiary`, `loadActiveUserProfile`, `checkUserExistsForNotification`처럼 호출 목적을 드러내는 이름을 우선합니다.
- Cross-Context Port는 대상 도메인의 Repository, Entity 구조, 조회 기술을 노출하지 않고 호출 도메인이 필요로 하는 정보와 목적을 기준으로 정의합니다.
- 저장소 조회 기준, JPA 사용 여부, 캐시 적용 여부 등 인프라 세부사항의 변경 영향은 Adapter 구현 내부에서 흡수되어야 합니다.

## 6. 다른 도메인에서 호출하는 방법

타 도메인의 데이터나 동작이 필요할 경우, 직접적으로 타 도메인의 엔티티나 DB(Repository)에 접근하지 않고 **Cross-Context Adapter**를 사용합니다.

### `crosscontext` 폴더 방식 (표준)

- **소유권**: 타 도메인의 정보나 동작이 필요한 **호출 도메인**이 Cross-Context Out Port와 Adapter를 소유합니다.
- **Out Port 정의**: 호출 도메인의 `application/port/out` 패키지에 호출 목적을 드러내는 Port 인터페이스를 정의합니다. Port의 입력과 출력은 호출 도메인이 필요로 하는 정보와 의미를 기준으로 설계합니다.
- **Adapter 구현**: 호출 도메인의 `adapter/out/crosscontext` 패키지에서 해당 Out Port를 구현합니다.
- **대상 도메인 호출**: Cross-Context Adapter는 대상 도메인이 `application/port/in`으로 공개한 In Port(UseCase)를 주입받아 데이터 조회나 동작을 요청합니다.
- **금지 의존성**: 호출 도메인의 Application Service와 Cross-Context Adapter는 대상 도메인의 Entity, `application/port/out`, Repository, Persistence Adapter에 직접 의존하지 않습니다.
- **호출 흐름**: 호출 도메인의 Application Service가 호출 도메인의 Out Port를 사용하고, Cross-Context Adapter가 이를 대상 도메인의 In Port 호출로 변환합니다. 대상 도메인은 자신의 Application Service와 Out Port를 통해 요청을 처리합니다.

### 기존 대상 도메인명 폴더 방식의 처리

- `adapter/out/{대상도메인명}` 방식은 더 이상 허용되는 대안이 아니며 `crosscontext` 폴더 방식으로의 마이그레이션 대상입니다.
- 신규 Cross-Context 연동은 반드시 `crosscontext` 폴더 방식을 사용합니다.
- 기존 연동 코드를 변경할 때에는 관련 Adapter를 `adapter/out/crosscontext`로 이동하고, 대상 도메인의 공개 In Port를 사용하도록 함께 전환합니다.
- 하위 호환성 등의 이유로 즉시 전환할 수 없다면 예외 사유, 영향 범위, 제거 조건을 관련 설계 또는 마이그레이션 문서에 기록해야 하며 예외 범위를 확장하지 않습니다.
