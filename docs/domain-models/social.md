# Social 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-02

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
- 발신자가 동일 수신자에게 이미 친구 요청을 보낸 상태에서 다시 요청하거나 동시 중복 저장이 발생한 경우 새 친구 요청 이벤트 없이 요청 완료 흐름으로 처리
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

### 행위

- `FriendRequest(String fromUserId, String toUserId)` : 특정 사용자가 다른 특정 사용자에게 보내는 친구 요청을 생성한다.

### 규칙

- 발신자(`fromUserId`)와 수신자(`toUserId`)의 조합(`FriendRequestID`)으로 단방향 요청을 식별한다.
- 발신자가 수신자에게 보내는 요청과 수신자가 발신자에게 보내는 요청은 서로 다른 단방향 요청이다.
- 수신자가 발신자에게 이미 요청한 상태에서 발신자가 수신자에게 요청하면 새 요청이 아니라 친구 수락으로 처리한다.
- 발신자가 동일 수신자에게 이미 요청한 상태에서 다시 요청하면 새 요청이나 새 알림 이벤트를 만들지 않고 요청 완료 흐름으로 처리한다.
- 친구 수락이 완료되어 `Friend` 엔티티가 생성되거나, 거절될 경우 해당 요청(엔티티)의 라이프사이클이 종료되거나 상태가 전환되어야 한다.
- 친구 요청은 대기 상태 자체가 도메인 의미이므로, 수락·거절·철회 시 요청 행을 물리적으로 삭제하여 라이프사이클을 종료한다.

---

## 댓글(Comment)

_Entity_

### 속성

- `id` : Long. 댓글의 고유 식별자
- `content` : String. 댓글 내용
- `userId` : String (UUID 36자리). 댓글 작성자 식별자
- `diaryId` : Long. 댓글이 속한 일기 식별자
- `parent` : Comment. 부모 댓글. 루트 댓글이면 `null`
- `children` : List<Comment>. 이 댓글에 달린 답글 목록
- `createdAt` : LocalDateTime. 댓글 생성 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 댓글 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. `Comment`가 소유하는 댓글 삭제 일시

### 행위

- `Comment(String content, String userId, Long diaryId)` : 특정 사용자가 특정 일기에 댓글을 생성한다.
- `connectParent(Comment parent)` : 부모 댓글을 연결하거나 변경한다.
- `updateContent(String content)` : 댓글 내용을 수정한다.
- `delete()` : 댓글을 소프트 삭제하고 삭제 일시를 기록한다.
- `isDeleted()` : 삭제된 댓글인지 여부를 반환한다.

### 규칙

- 댓글은 정확히 하나의 일기(`diaryId`)에 종속된다.
- `parent == null`이면 루트 댓글, `parent != null`이면 답글이다.
- 답글은 동일한 일기 안의 댓글에만 연결될 수 있다.
- 서비스 정책상 답글의 답글은 허용하지 않는다. 즉 댓글 계층은 최대 2단(댓글 / 답글)까지만 허용된다.
- 삭제는 물리 삭제가 아니라 소프트 삭제로 처리되며, 삭제된 댓글은 조회 시 별도 정책을 따른다.

---

## 좋아요(Like)

_Entity_

### 속성

- `id` : Long. 좋아요의 고유 식별자
- `userId` : String (UUID 36자리). 좋아요를 누른 사용자 식별자
- `diaryId` : Long. 좋아요 대상 일기 식별자
- `createdAt` : LocalDateTime. 좋아요 생성 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. `Like`가 소유하는 취소 처리 일시

### 행위

- `Like.builder()` : 사용자와 일기를 기준으로 좋아요를 생성한다.
- `cancel()` : 좋아요를 취소 처리하고 `deletedAt`을 현재 시각으로 설정한다.
- `reactivate()` : 취소된 좋아요를 다시 활성 상태로 되돌리고 `deletedAt`을 비운다.
- `isActive()` : 좋아요가 현재 활성 상태인지 반환한다.

### 규칙

- 하나의 사용자(`userId`)는 하나의 일기(`diaryId`)에 active 좋아요를 한 번만 가질 수 있다.
- 좋아요 중복 방지는 `(userId, diaryId)` 유니크 제약으로 보장한다.
- 좋아요 취소는 소프트 삭제로 처리되며, 재좋아요 시 기존 `Like`를 재활성화하여 유니크 제약을 유지한다.

---

## 댓글과 좋아요 접근 정책

### FRIENDS 일기 상호작용 정책

- `FRIENDS` 공개 범위 일기의 댓글과 답글은 작성자 본인 및 현재 친구 관계인 사용자만 조회할 수 있다.
- 비친구는 `FRIENDS` 일기의 댓글 목록, 답글 목록, 댓글 수, 좋아요 상태, 좋아요 수에 접근할 수 없다.
- 비친구는 `diaryId` 기반 댓글/좋아요 API에서 숨겨진 일기의 내용과 상호작용 정보를 조회할 수 없다.
- `commentId` 또는 `parentCommentId`를 직접 아는 경우의 접근은 애플리케이션 에러 정책을 따르며, 존재 은닉 자체를 1차 목표로 두지는 않는다.

### PRIVATE 일기 상호작용 정책

- `PRIVATE` 일기의 댓글과 좋아요는 작성자 본인만 접근할 수 있다.
- 비소유자는 `PRIVATE` 일기의 댓글 목록, 답글 목록, 댓글 수, 좋아요 상태, 좋아요 수에 접근할 수 없다.
- `commentId` 직접 접근 경로의 세부 에러는 애플리케이션 정책을 따르되, 숨겨진 일기의 본문이나 목록 응답은 노출되면 안 된다.
