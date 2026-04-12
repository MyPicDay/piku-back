# DDD + 헥사고날 아키텍처 문서

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-04-12

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

## 5. 다른 도메인에서 호출하는 방법

타 도메인의 데이터나 동작이 필요할 경우, 직접적으로 타 도메인의 엔티티나 DB(Repository)에 접근하지 않고 **Cross-Context Adapter**를 사용합니다.

### 패턴 A: `crosscontext` 폴더 방식 (권장)

- **Out Port 정의**: 해당 도메인의 `application/port/out` 패키지에 타 도메인의 정보 획득을 위한 Port 인터페이스 (예: `LoadUserForDiaryPort`)를 정의합니다.
- **Adapter 구현**: `adapter/out/crosscontext` 패키지에서 해당 Port 인터페이스를 구현합니다.
- **호출 방식**: 어댑터 내에서는 Spring의 DI를 활용하여 타 도메인의 공개된 빈(예: 타 도메인의 `Reader`, `QueryService`, 또는 `UseCase`)을 주입받아 데이터를 조회/명령합니다.
- **적용 도메인**: `diary`, `notification`, `social`, `support`

### 패턴 B: 대상 도메인명 폴더 방식

- `adapter/out/{대상도메인명}/` 형태로 타 도메인 연동 어댑터를 분리합니다.
- **적용 도메인**: `user` (`adapter/out/character/`, `adapter/out/diary/`, `adapter/out/friend/`)

> 두 패턴 모두 각 도메인의 Application Service 계층이 타 도메인에 대한 강결합을 피하고, Port 인터페이스에만 의존하는 목적은 동일합니다. 프로젝트 내 통일을 위해 **패턴 A**로 수렴하는 것을 권장합니다.
