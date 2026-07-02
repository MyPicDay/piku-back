# User 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-02

## 도메인 개요

User 도메인은 **서비스를 이용하는 모든 사용자의 식별 정보와 프로필을 관리**하는 도메인입니다. 시스템 전반에서 주체(Actor)가 되는 사용자 개념을 정의하며, 프로필 변경의 비즈니스 규칙을 캡슐화합니다.

### 목적

- 서비스 내의 모든 행위 주체(`User`)를 고유하게 식별하고 인증할 수 있는 기반을 제공한다.
- 닉네임, 아바타 등 프로필 정보의 변경 규칙을 도메인 객체(`User`) 내에 캡슐화하여 일관성을 보장한다.
- 닉네임 형식(1~20자), 이메일 형식 등 값 객체(Value Object)를 통해 입력 데이터의 정합성을 강력하게 보장한다.

### 핵심 책임

- 사용자(`User`) 등록, 프로필 수정, 회원 탈퇴(논리적 삭제) 관리
- 닉네임 점유(`NicknameHold`) 메커니즘: 중복 확인 시점부터 수정 확정까지 일정 시간 동안 닉네임을 점유하여 동시 요청에 의한 충돌 방지
- `Character` 도메인의 고정 캐릭터 이미지(`characterId`)를 기반으로 아바타 URL을 결정하여 변경
- `NicknamePolicy` 도메인 서비스를 통한 닉네임 점유 만료 정책 관리
- 닉네임 키워드 기반 사용자 검색

### 도메인 경계

- **Aggregate Root**: `User`
- `Email`, `Nickname`, `Avatar`는 `User` 내의 Value Object로, 자체 유효성 규칙을 가지며 `User` 없이 독립 존재하지 않는다.
- `NicknamePolicy`는 닉네임 점유 만료 여부 판단 로직을 분리한 **도메인 서비스**이다.
- 인증 토큰(JWT 등)의 발급 및 검증은 `security`, `auth` 패키지에서 처리하며, User 도메인의 책임 범위 밖이다.
- 아바타 이미지 원본은 `Character` 도메인(`LoadCharacterPort`)에서 관리한다.

### 타 도메인과의 관계

| 도메인           | 관계 설명                                                                                     |
| ---------------- | --------------------------------------------------------------------------------------------- |
| **Diary**        | `userId`로 일기 작성자를 식별하고, `LoadUserForDiaryPort`를 통해 사용자 존재 여부를 확인한다. |
| **Social**       | `LoadUserInfoPort`를 통해 친구 관계에서의 사용자 닉네임·아바타 정보를 제공한다.               |
| **Notification** | `LoadUserForNotificationPort`를 통해 알림 응답에 발신자 닉네임·아바타를 제공한다.             |
| **Feed**         | `LoadUserForFeedPort`를 통해 피드 응답에 일기 작성자의 닉네임·아바타를 제공한다.              |
| **Character**    | `LoadCharacterPort`를 통해 고정 캐릭터 이미지 URL을 조회하여 아바타를 결정한다.               |

### 유스케이스 접근 정책

- 프로필 미리보기 조회는 인증 없이 수행할 수 있다.
- 프로필 상세 조회는 인증된 사용자만 수행할 수 있다.
- 프로필 수정과 프로필 이미지 변경은 본인만 수행할 수 있다.
- 프로필 상세와 미리보기에 포함되는 일기 수 및 프로필 상세의 전체 월별 일기 수는 `Diary` 도메인의 공개 범위 정책을 따른다.
- 프로필 조회 결과에 포함되는 친구 상태와 본인 여부는 현재 조회 요청자와 프로필 대상 사용자의 관계를 기준으로 결정된다.

---

## 사용자(User)

_Entity_

### 속성

- `id` : String (UUID 36자리). 사용자의 고유 식별자
- `email` : String. 로그인 및 식별용 이메일 주소 (Unique, Not Null)
- `password` : String. 해시 암호화 처리된 사용자 비밀번호
- `nickname` : String. 서비스 내에서 다른 사람에게 보여지는 고유한 표시 이름 (Unique, Not Null)
- `avatar` : String. 사용자의 프로필 이미지 경로
- `createdAt` : LocalDateTime. 최초 가입/생성 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 최종 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. `User`가 소유하는 회원 탈퇴 처리 일시

### 행위

- `changeNickname(String newNickname)` : 닉네임을 변경한다.
- `changeAvatar(String avatar)` : 프로필 이미지 경로를 변경한다.
- `updatePassword(String newHashedPassword)` : 암호화된 새로운 비밀번호로 변경한다.
- `withdraw()` : 사용자를 회원 탈퇴 처리하고 `deletedAt`을 현재 시각으로 설정한다.
- `isWithdrawn()` : 회원 탈퇴 처리된 사용자인지 여부를 반환한다.

### 규칙

- 이메일(`email`)과 닉네임(`nickname`)은 전체 시스템 내에서 고유(Unique)해야 하며 필수값이다.
- 사용자 객체의 상태 변경(비밀번호, 닉네임, 아바타 등)은 캡슐화된 메서드를 통해서만 제어되어야 한다.
- 회원 탈퇴는 물리적 완전 삭제 대신 `User`가 소유한 `deletedAt` 값을 갖는 논리적 삭제 구조로 처리한다.

---

## 닉네임(Nickname)

_Value Object_

### 속성

- `value` : String. 닉네임 문자열

### 행위

- `Nickname(String value)` : 검증 로직을 포함하여 객체를 생성한다. (Record 기반)

### 규칙

- 닉네임은 필수 값(Not Null, Not Blank)이다.
- 길이는 최소 1자에서 최대 20자 사이여야 한다. 조건을 만족하지 않으면 예외가 발생한다.

---

## 이메일(Email)

_Value Object_

### 속성

- `value` : String. 이메일 문자열

### 행위

- `Email(String value)` : 검증 로직을 포함하여 객체를 생성한다. (Record 기반)

### 규칙

- 이메일은 필수 값(Not Null, Not Blank)이다.
- 표준 이메일 정규식 패턴(`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`)을 통과해야 한다. 조건을 만족하지 않으면 예외가 발생한다.

---

## 아바타(Avatar)

_Value Object_

### 속성

- `path` : String. 프로필 이미지의 저장소 경로

### 행위

- `isEmpty()` : 아바타 경로가 비어 있는지 확인한다.
- `isSameAs(Avatar other)` : 다른 아바타 객체와 동일한 이미지 경로인지 비교한다.

### 규칙

- 비어있는 문자열 상태를 별도로 확인 및 검사할 수 있다.
- 두 아바타 객체가 논리적으로 동일한지(`path` 일치 여부) 판별할 수 있다.
