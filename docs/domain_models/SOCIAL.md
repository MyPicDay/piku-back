# Social 도메인 모델

## 도메인 개요

Social 도메인은 **사용자 간의 친구 관계뿐 아니라 댓글, 좋아요 등 컨텐츠 상호작용을 모델링하고 관리**하는 도메인입니다. 관계 발생 → 이벤트 발행 → 알림 트리거의 흐름을 담당하며, 다른 도메인에서 공개 범위 판단 및 피드 구성의 기준점이 됩니다.

### 목적

- 사용자 간 양방향 친구 관계의 성립 과정(요청 → 수락/자동 수락/거절/철회)을 명확하게 모델링한다.
- `FriendStatus`를 통해 두 사용자 간의 현재 소셜 컨텍스트(NONE / REQUESTED / RECEIVED / FRIENDS)를 조회할 수 있도록 한다.
- 댓글(`Comment`)과 좋아요(`Like`)를 통해 일기에 대한 상호작용 컨텐츠를 관리한다.
- 친구 관계 이벤트를 `PublishEventPort`로 발행하여 Notification 도메인이 알림을 비동기 트리거하도록 한다.

### 핵심 책임

- 친구 요청(`FriendRequest`) 생성, 수락, 거절, 철회 관리
- 상대방이 이미 요청을 보낸 경우 자동으로 친구 수락하고 `Friend` 엔티티 생성
- 수락된 양방향 친구 관계(`Friend`) 생성, 해제 관리
- 두 사용자 간 현재 관계 상태(`FriendStatus`) 조회
- 친구 수 카운트 및 친구 ID 목록 반환
- 댓글(`Comment`) 작성, 답글(계층 구조), 수정, 소프트 삭제 관리
- 좋아요(`Like`) 토글 및 일기별 좋아요 수 조회
- 소셜 이벤트(`SocialEvent.FriendRequestEvent`, `SocialEvent.FriendAcceptedEvent`) 발행

### 도메인 경계

- **Aggregate Root**: `FriendRequest`, `Friend`, `Comment`, `Like` (각각 독립적 Aggregate)
- `FriendRequest`가 수락되면 물리적으로 삭제(`saveFriendRequestPort.delete`)되고 새 `Friend` Aggregate가 생성된다.
- `Comment`는 `parent` 자기 참조 관계로 계층 구조(댓글 / 답글)를 지원한다.
- `Like`는 테이블 레벨에서 `(userId, diaryId)` 유니크 제약으로 중복 좋아요를 방지한다.
- 사용자 상세 정보는 `LoadUserInfoPort`를 통해 User 도메인에 위임하며, Social 도메인은 `userId` 참조만 보유한다.
- 친구 이벤트 발행은 `PublishEventPort`를 통해 처리하며, 실제 알림 세부 로직은 Notification 도메인이 담당한다.

### 타 도메인과의 관계

| 도메인           | 관계 설명                                                                                                      |
| ---------------- | -------------------------------------------------------------------------------------------------------------- |
| **User**         | `LoadUserInfoPort`로 친구·댓글 응답에 닉네임·아바타를 조회한다.                                                |
| **Diary**        | `diaryId`를 참조하여 댓글/좋아요의 대상 일기를 식별한다. 친구 관계가 `FRIENDS` 공개 범위 접근 여부를 결정한다. |
| **Notification** | `PublishEventPort`로 소셜 이벤트를 발행하면 Notification 도메인이 구독하여 알림을 생성한다.                    |
| **Feed**         | `LoadSocialForFeedPort`로 친구 관계·좋아요·댓글 수를 Feed 응답 DTO에 제공한다.                                 |

---

## 친구 상태(FriendStatus)

_Enum_

### 상수

- `NONE` : 아무 관계가 없는 상태
- `REQUESTED` : 친구 요청을 보낸 주체의 관점에서 대기 중인 상태
- `RECEIVED` : 친구 요청을 받은 대상의 관점에서 대기 중인 상태
- `FRIENDS` : 서로 친구 요청을 수락하여 양방향 친구가 된 상태

---

## 친구 자격(Friend)

_Entity_

### 속성

- `userId1` : String (UUID 36자리). 친구 관계의 주체 사용자 식별자 (복합키 1)
- `userId2` : String (UUID 36자리). 친구 관계의 대상 사용자 식별자 (복합키 2)
- `createdAt` : String. 친구 관계가 성립된 일시

### 행위

- `Friend(String userId1, String userId2)` : 두 사용자 간의 친구 관계를 생성한다.

### 규칙

- `userId1`과 `userId2`의 조합(`FriendID`)으로 하나의 고유한 친구 관계를 식별한다.
- 맺어진 관계(생성일시)는 수정(`updatable = false`)할 수 없다.

---

## 친구 요청(FriendRequest)

_Entity_

### 속성

- `fromUserId` : String (UUID 36자리). 친구 요청을 발신한 사용자 식별자 (복합키 1)
- `toUserId` : String (UUID 36자리). 친구 요청을 수신한 사용자 식별자 (복합키 2)
- `updatedAt` : LocalDateTime. 최종 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. 삭제 처리 일시 (`BaseEntity` 공통, 소프트 삭제용)

### 행위

- `FriendRequest(String fromUserId, String toUserId)` : 특정 사용자가 다른 특정 사용자에게 보내는 친구 요청을 생성한다.
- `inactive()` : 요청을 논리적 삭제 처리(소프트 삭제)한다. (`BaseEntity` 공통)

### 규칙

- 발신자(`fromUserId`)와 수신자(`toUserId`)의 조합(`FriendRequestID`)으로 단방향 요청을 식별한다.
- 친구 수락이 완료되어 `Friend` 엔티티가 생성되거나, 거절될 경우 해당 요청(엔티티)의 라이프사이클이 종료되거나 상태가 전환되어야 한다.
- 요청 철회/거절 시 실제 데이터 삭제 대신 `deletedAt` 값을 설정하는 논리적 삭제 구조를 따른다.

---

## 댓글과 좋아요 접근 정책

### FRIENDS 일기 상호작용 정책

- `FRIENDS` 공개 범위 일기의 댓글과 답글은 작성자 본인 및 현재 친구 관계인 사용자만 조회할 수 있다.
- 비친구는 `FRIENDS` 일기의 댓글 목록, 답글 목록, 댓글 수, 좋아요 상태, 좋아요 수에 접근할 수 없다.
- 비친구는 `commentId` 또는 `parentCommentId`를 알고 있어도 댓글 조회, 답글 조회, 수정, 삭제를 통해 존재를 유추할 수 없어야 한다.
- 따라서 비친구의 `FRIENDS` 일기 댓글/답글 직접 접근은 모두 `404 Not Found`로 처리하여 존재를 숨긴다.

### PRIVATE 일기 상호작용 정책

- `PRIVATE` 일기의 댓글과 좋아요는 작성자 본인만 접근할 수 있다.
- 비소유자는 `PRIVATE` 일기의 댓글/좋아요 API를 통해 `diaryId`, `commentId`, 답글 존재 여부를 유추할 수 없어야 한다.
- 따라서 비소유자의 `PRIVATE` 일기 댓글/좋아요 접근도 모두 `404 Not Found`로 처리한다.
